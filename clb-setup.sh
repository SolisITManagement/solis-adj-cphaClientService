#!/usr/bin/env bash
set -euo pipefail

# ─── Configuration ───
PROJECT_ID="solis-public"
REGION="northamerica-northeast1"

# Cloud Run service names (must match deployed services)
DIS_SERVICE="solis-dis-dataservice"
ADJ_SERVICE="solis-adj-dataservice"

# Resource names
STATIC_IP="solis-dis-lb-ip"
NEG_DIS="solis-dis-dataservice-neg"
NEG_ADJ="solis-adj-dataservice-neg"
BACKEND_DIS="solis-dis-backend"
BACKEND_ADJ="solis-adj-backend"
URL_MAP="solis-dis-url-map"
ARMOR_POLICY="solis-dis-security-policy"
HTTPS_PROXY="solis-dis-https-proxy"
FWD_RULE="solis-dis-https-fr"

# SSL cert (self-managed, self-signed). Covers both DIS and ADJ backends since
# both sit behind the same LB / URL map on the same IP.
SSL_CERT="solis-dis-and-adj-cert"
# Paths to cert + key PEMs used only when creating the cert the first time.
# After upload the operator is expected to shred the key file; subsequent runs
# skip the create-cert block because the cert already exists in GCP.
CERT_PEM="${CERT_PEM:-solis-dis-and-adj-cert.pem}"
KEY_PEM="${KEY_PEM:-solis-dis-and-adj-key.pem}"

# API key — fetched from Secret Manager at runtime
SECRET_NAME="solis-dis-gateway-api-key"
API_KEY=$(gcloud secrets versions access latest \
    --secret="${SECRET_NAME}" \
    --project="${PROJECT_ID}")
echo "    API key loaded from Secret Manager (${SECRET_NAME})."

echo "============================================"
echo " Solis DIS — Cloud Load Balancer + Cloud Armor Setup"
echo " Project : ${PROJECT_ID}"
echo " Region  : ${REGION}"
echo "============================================"

# ─── Step 1: Enable required APIs ───
echo ""
echo ">>> Step 1: Enabling required APIs..."
gcloud services enable compute.googleapis.com --project="${PROJECT_ID}"
echo "    compute.googleapis.com enabled."

# ─── Step 2: Reserve a global static IP ───
echo ""
echo ">>> Step 2: Reserving global static IP '${STATIC_IP}'..."
if gcloud compute addresses describe "${STATIC_IP}" --global --project="${PROJECT_ID}" &>/dev/null; then
    echo "    IP '${STATIC_IP}' already exists — skipping."
else
    gcloud compute addresses create "${STATIC_IP}" \
        --global \
        --project="${PROJECT_ID}"
    echo "    Static IP reserved."
fi

LB_IP=$(gcloud compute addresses describe "${STATIC_IP}" \
    --global --project="${PROJECT_ID}" \
    --format="value(address)")
echo "    Load Balancer IP: ${LB_IP}"

# ─── Step 3: Create Serverless NEGs ───
echo ""
echo ">>> Step 3: Creating Serverless NEGs..."

if gcloud compute network-endpoint-groups describe "${NEG_DIS}" --region="${REGION}" --project="${PROJECT_ID}" &>/dev/null; then
    echo "    NEG '${NEG_DIS}' already exists — skipping."
else
    gcloud compute network-endpoint-groups create "${NEG_DIS}" \
        --region="${REGION}" \
        --network-endpoint-type=serverless \
        --cloud-run-service="${DIS_SERVICE}" \
        --project="${PROJECT_ID}"
    echo "    NEG '${NEG_DIS}' created."
fi

if gcloud compute network-endpoint-groups describe "${NEG_ADJ}" --region="${REGION}" --project="${PROJECT_ID}" &>/dev/null; then
    echo "    NEG '${NEG_ADJ}' already exists — skipping."
else
    gcloud compute network-endpoint-groups create "${NEG_ADJ}" \
        --region="${REGION}" \
        --network-endpoint-type=serverless \
        --cloud-run-service="${ADJ_SERVICE}" \
        --project="${PROJECT_ID}"
    echo "    NEG '${NEG_ADJ}' created."
fi

# ─── Step 4: Create Backend Services ───
echo ""
echo ">>> Step 4: Creating backend services..."

if gcloud compute backend-services describe "${BACKEND_DIS}" --global --project="${PROJECT_ID}" &>/dev/null; then
    echo "    Backend '${BACKEND_DIS}' already exists — skipping."
else
    gcloud compute backend-services create "${BACKEND_DIS}" \
        --global \
        --load-balancing-scheme=EXTERNAL_MANAGED \
        --project="${PROJECT_ID}"
    gcloud compute backend-services add-backend "${BACKEND_DIS}" \
        --global \
        --network-endpoint-group="${NEG_DIS}" \
        --network-endpoint-group-region="${REGION}" \
        --project="${PROJECT_ID}"
    echo "    Backend '${BACKEND_DIS}' created with NEG."
fi

if gcloud compute backend-services describe "${BACKEND_ADJ}" --global --project="${PROJECT_ID}" &>/dev/null; then
    echo "    Backend '${BACKEND_ADJ}' already exists — skipping."
else
    gcloud compute backend-services create "${BACKEND_ADJ}" \
        --global \
        --load-balancing-scheme=EXTERNAL_MANAGED \
        --project="${PROJECT_ID}"
    gcloud compute backend-services add-backend "${BACKEND_ADJ}" \
        --global \
        --network-endpoint-group="${NEG_ADJ}" \
        --network-endpoint-group-region="${REGION}" \
        --project="${PROJECT_ID}"
    echo "    Backend '${BACKEND_ADJ}' created with NEG."
fi

# ─── Step 5: Create Cloud Armor Security Policy ───
echo ""
echo ">>> Step 5: Creating Cloud Armor security policy '${ARMOR_POLICY}'..."

if gcloud compute security-policies describe "${ARMOR_POLICY}" --project="${PROJECT_ID}" &>/dev/null; then
    echo "    Policy '${ARMOR_POLICY}' already exists — skipping creation."
else
    gcloud compute security-policies create "${ARMOR_POLICY}" \
        --project="${PROJECT_ID}" \
        --description="Solis DIS security policy — API key validation, rate limiting, WAF"
    echo "    Policy created."
fi

# Rule 100: Allow requests with valid x-api-key header
echo "    Adding API key validation rule (priority 100)..."
gcloud compute security-policies rules create 100 \
    --security-policy="${ARMOR_POLICY}" \
    --expression="request.headers['x-api-key'] == '${API_KEY}'" \
    --action=allow \
    --description="Allow requests with valid API key" \
    --project="${PROJECT_ID}" 2>/dev/null || echo "    Rule 100 already exists — skipping."

# Default rule: Deny all requests without a valid API key
echo "    Setting default rule to deny-403..."
gcloud compute security-policies rules update 2147483647 \
    --security-policy="${ARMOR_POLICY}" \
    --action=deny-403 \
    --description="Default deny — requests without valid API key are rejected" \
    --project="${PROJECT_ID}"

# Attach policy to both backend services
echo "    Attaching policy to backend services..."
gcloud compute backend-services update "${BACKEND_DIS}" \
    --global \
    --security-policy="${ARMOR_POLICY}" \
    --project="${PROJECT_ID}"
gcloud compute backend-services update "${BACKEND_ADJ}" \
    --global \
    --security-policy="${ARMOR_POLICY}" \
    --project="${PROJECT_ID}"
echo "    Cloud Armor policy attached."

# ─── Step 6: Create URL Map (path-based routing) ───
echo ""
echo ">>> Step 6: Creating URL map '${URL_MAP}'..."

if gcloud compute url-maps describe "${URL_MAP}" --project="${PROJECT_ID}" &>/dev/null; then
    echo "    URL map '${URL_MAP}' already exists — skipping."
else
    # Default backend is adjudication (handles /, /publish, /totals, /retry*)
    gcloud compute url-maps create "${URL_MAP}" \
        --default-service="${BACKEND_ADJ}" \
        --global \
        --project="${PROJECT_ID}"

    # Add path matcher: /api/dis/* goes to DIS backend, everything else to ADJ backend
    gcloud compute url-maps add-path-matcher "${URL_MAP}" \
        --path-matcher-name="dis-routes" \
        --default-service="${BACKEND_ADJ}" \
        --path-rules="/api/dis/*=${BACKEND_DIS}" \
        --global \
        --project="${PROJECT_ID}"

    echo "    URL map created with path-based routing."
fi

# ─── Step 7: Upload SSL certificate ───
echo ""
echo ">>> Step 7: Uploading self-managed SSL certificate '${SSL_CERT}'..."

if gcloud compute ssl-certificates describe "${SSL_CERT}" --global --project="${PROJECT_ID}" &>/dev/null; then
    echo "    Certificate '${SSL_CERT}' already exists — skipping upload."
else
    if [[ ! -f "${CERT_PEM}" || ! -f "${KEY_PEM}" ]]; then
        echo "    ERROR: ${CERT_PEM} or ${KEY_PEM} not found." >&2
        echo "    Generate a self-signed cert first (run once per LB IP):" >&2
        echo "      LB_IP=\$(gcloud compute addresses describe ${STATIC_IP} --global --project=${PROJECT_ID} --format='value(address)')" >&2
        echo "      openssl req -x509 -nodes -newkey rsa:2048 -days 825 \\" >&2
        echo "        -keyout ${KEY_PEM} -out ${CERT_PEM} \\" >&2
        echo "        -subj \"/CN=\${LB_IP}/O=Solis/OU=DIS-and-ADJ\" \\" >&2
        echo "        -addext \"subjectAltName=IP:\${LB_IP}\"" >&2
        exit 1
    fi
    gcloud compute ssl-certificates create "${SSL_CERT}" \
        --certificate="${CERT_PEM}" \
        --private-key="${KEY_PEM}" \
        --global \
        --project="${PROJECT_ID}"
    echo "    Certificate uploaded. REMEMBER to shred the local key: shred -u ${KEY_PEM}"
fi

# ─── Step 8: Create HTTPS Target Proxy ───
echo ""
echo ">>> Step 8: Creating HTTPS target proxy '${HTTPS_PROXY}'..."

if gcloud compute target-https-proxies describe "${HTTPS_PROXY}" --project="${PROJECT_ID}" &>/dev/null; then
    echo "    Proxy '${HTTPS_PROXY}' already exists — skipping."
else
    gcloud compute target-https-proxies create "${HTTPS_PROXY}" \
        --url-map="${URL_MAP}" \
        --ssl-certificates="${SSL_CERT}" \
        --global \
        --project="${PROJECT_ID}"
    echo "    HTTPS proxy created (bound to ${SSL_CERT})."
fi

# ─── Step 9: Create Forwarding Rule (port 443 only; port 80 intentionally closed) ───
echo ""
echo ">>> Step 9: Creating HTTPS forwarding rule '${FWD_RULE}' on port 443..."

if gcloud compute forwarding-rules describe "${FWD_RULE}" --global --project="${PROJECT_ID}" &>/dev/null; then
    echo "    Forwarding rule '${FWD_RULE}' already exists — skipping."
else
    gcloud compute forwarding-rules create "${FWD_RULE}" \
        --global \
        --address="${STATIC_IP}" \
        --target-https-proxy="${HTTPS_PROXY}" \
        --ports=443 \
        --load-balancing-scheme=EXTERNAL_MANAGED \
        --project="${PROJECT_ID}"
    echo "    Forwarding rule created."
fi

# ─── Summary ───
echo ""
echo "============================================"
echo " SETUP COMPLETE"
echo ""
echo " Load Balancer IP : ${LB_IP}"
echo " Test URL (DIS)   : https://${LB_IP}/api/dis/bc/publish"
echo " Test URL (ADJ)   : https://${LB_IP}/publish"
echo ""
echo " API Key source: Secret Manager (${SECRET_NAME})"
echo " SSL cert      : ${SSL_CERT} (self-signed, SAN=IP:${LB_IP})"
echo ""
echo " Example curl test (-k required because the cert is self-signed):"
echo "   curl -k -X POST https://${LB_IP}/api/dis/bc/publish \\"
echo "     -H 'Content-Type: application/json' \\"
echo "     -H 'x-api-key: \$(gcloud secrets versions access latest --secret=${SECRET_NAME} --project=${PROJECT_ID})' \\"
echo "     -d '{\"test\": true}'"
echo ""
echo " Next steps:"
echo "   1. Export GATEWAY_HOST=${LB_IP} (base-url already uses https://)"
echo "   2. Build the client trust store once and drop it at ~/.solis/solis-truststore.jks"
echo "      (picked up automatically by TrustStoreEnvironmentPostProcessor; no -D flags needed):"
echo "        cp \"\$JAVA_HOME/lib/security/cacerts\" solis-truststore.jks && chmod 644 solis-truststore.jks"
echo "        keytool -importcert -noprompt -trustcacerts -alias solis-dis-and-adj-lb \\"
echo "          -file ${CERT_PEM} -keystore solis-truststore.jks -storepass changeit"
echo "        mkdir -p ~/.solis && mv solis-truststore.jks ~/.solis/solis-truststore.jks"
echo "   3. API key is auto-loaded from Secret Manager via sm:// — no env var needed"
echo "   4. Lock down Cloud Run services:"
echo "      gcloud run services update ${DIS_SERVICE} --region=${REGION} --ingress=internal-and-cloud-load-balancing --project=${PROJECT_ID}"
echo "      gcloud run services update ${ADJ_SERVICE} --region=${REGION} --ingress=internal-and-cloud-load-balancing --project=${PROJECT_ID}"
echo "============================================"
