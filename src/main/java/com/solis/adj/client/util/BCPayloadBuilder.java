package com.solis.adj.client.util;

import java.security.SecureRandom;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Builds the full BC PharmaNet / eHealth JSON payload from
 * simplified business data (Patient + Prescription) by merging
 * infrastructure defaults (Header, Sender, Receiver, MSH, ZCA, ZCB,
 * ZCC, ZPX, ZCD, ZPJ, ZZZ, ZZZSegments, Candidate, Event, Visit).
 */
public final class BCPayloadBuilder {

	private static final DateTimeFormatter DATE_FORMATTER =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

	public record EventConfig(String eventTypeCode, String interactionId, String hl7Version) {}

	public static final Map<String, EventConfig> EVENT_CONFIG = Map.ofEntries(
		Map.entry("bc-find-candidate",      new EventConfig("HCIM_IN_FindCandidates", "HCIM_IN_FindCandidates",  "V3PR1")),
		Map.entry("bc-get-demographics",    new EventConfig("HCIM_IN_GetCandidate",   "HCIM_IN_GetDemographics", "V3PR1")),
		Map.entry("bc-patient-profile",     new EventConfig("TRP",              "TRP",              "V2PR1")),
		Map.entry("bc-prescriber-id",       new EventConfig("TIP",              "TIP",              "V2PR1")),
		Map.entry("bc-claim-reversal",      new EventConfig("TAC_TDU_REVERSAL", "TAC_TDU_REVERSAL", "V2PR1")),
		Map.entry("bc-retrieve-rx",         new EventConfig("TRX_X0",           "TRX_X0",           "V2PR1")),
		Map.entry("bc-record-rx",           new EventConfig("TRX_X1",           "TRX_X1",           "V2PR1")),
		Map.entry("bc-profile-info-update", new EventConfig("TPI",              "TPI",              "V2PR1")),
		Map.entry("bc-update-rx-status",    new EventConfig("TRX_X2",           "TRX_X2",           "V2PR1")),
		Map.entry("bc-adjust-rx",           new EventConfig("TRX_X3",           "TRX_X3",           "V2PR1")),
		Map.entry("bc-location-details",    new EventConfig("TIL",              "TIL",              "V2PR1")),
		Map.entry("bc-dispense-event",      new EventConfig("DISPENSE",         "DISPENSE",         "V2PR1")),
		Map.entry("bc-adj-reconciliation",         new EventConfig("TDT",              "TDT",              "V2PR1")),
		Map.entry("bc-protective-word",            new EventConfig("TCP",              "TCP",              "V2PR1")),
		Map.entry("bc-keyword-verify",             new EventConfig("TPA",              "TPA",              "V2PR1")),
		Map.entry("bc-medication-update",          new EventConfig("TMU",              "TMU",              "V2PR1")),
		Map.entry("bc-medication-update-reversal", new EventConfig("TMU_REVERSAL",     "TMU_REVERSAL",     "V2PR1")),
		Map.entry("bc-phn-assignment",             new EventConfig("TPH",              "TPH",              "V2PR1")),
		Map.entry("bc-patient-name-search",        new EventConfig("TPN",              "TPN",              "V2PR1"))
	);

	private BCPayloadBuilder() {}

	/** Validates a view name against the allowed set, falling back to bc-find-candidate. */
	public static String sanitizeViewName(String candidate) {
		return EVENT_CONFIG.containsKey(candidate) ? candidate : "bc-find-candidate";
	}

	/** Returns the simplified business-data JSON shown in the UI textarea. */
	public static String getBusinessDataDefault() {
		return """
				{
				  "Patient": {
				    "phn": "9876543210",
				    "firstName": "GENDER",
				    "lastName": "MASK",
				    "dob": "19800101",
				    "gender": "M",
				    "address": {
				      "line": "123 Main St",
				      "city": "Vancouver",
				      "province": "BC",
				      "postalCode": "V5K0A1"
				    },
				    "phone": "6045551234"
				  },
				  "Prescription": {
				    "dinGpPin": "02489007",
				    "quantity": "700",
				    "daysSupply": "14",
				    "productCost": "91",
				    "professionalFee": "99",
				    "newRefillCode": "N",
				    "prescriberId": "13133135",
				    "prescriptionNumber": "13147",
				    "sigInstructions": "APPLY TO FULL FACE ( EXCEPT AROUND EYES ) STARTING TWICE WEEKLY ( INCREASE TO..."
				  }
				}""";
	}

	/**
	 * Merges simplified business JSON with infrastructure defaults
	 * to produce the full payload expected by the Data Service.
	 */
	public static String buildFullPayload(String businessJson, String viewName) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode bizNode = mapper.readTree(businessJson);
		JsonNode patient = bizNode.path("Patient");
		JsonNode rx = bizNode.path("Prescription");

		EventConfig config = EVENT_CONFIG.get(viewName);
		String datetime = formatCurrentDateTime();
		String messageId = generateRandomId();

		ObjectNode root = mapper.createObjectNode();

		// Header - HL7 routing metadata (messageId, versionCode, interactionId, OIDs)
		root.set("Header",   buildHeader(mapper, messageId, config, datetime));
		// Sender - sending device/organization identifiers (static defaults)
		root.set("Sender",   buildSender(mapper));
		// Receiver - receiving device/organization identifiers (static defaults)
		root.set("Receiver", buildReceiver(mapper));
		// MSH - HL7 v2 message header (separators, encoding, facilities, security)
		root.set("MSH",      buildMSH(mapper, datetime));

		// Candidate (mapped from Patient business data - name, PHN, DOB, gender, address)
		String phn = patient.path("phn").asText("9876543210");
		root.set("Candidate", buildCandidate(mapper, patient, phn));
		// Event - event type code derived from the selected message + timestamp
		root.set("Event",     buildEvent(mapper, config, datetime));
		// Visit - patient class, location, provider (static defaults)
		root.set("Visit",     buildVisit(mapper, datetime));
		// ZCA - carrier/bin/software identifiers (static defaults)
		root.set("ZCA",       buildZCA(mapper));
		// ZCB - location/trace/transaction metadata (static defaults)
		root.set("ZCB",       buildZCB(mapper));
		// ZCC - PHN reference (derived from Patient business data)
		root.set("ZCC",       buildZCC(mapper, phn));

		// Extract Prescription fields from user's business data
		String dinGpPin         = rx.path("dinGpPin").asText("02489007");
		String quantity         = rx.path("quantity").asText("700");
		String daysSupply       = rx.path("daysSupply").asText("14");
		String productCost      = rx.path("productCost").asText("91");
		String professionalFee  = rx.path("professionalFee").asText("99");
		String newRefillCode    = rx.path("newRefillCode").asText("N");
		String prescriberId     = rx.path("prescriberId").asText("13133135");
		String sigInstructions  = rx.path("sigInstructions").asText("");
		String prescriptionNumber = rx.path("prescriptionNumber").asText("13147");

		// ZPX - prescription/drug details (mapped from Prescription business data)
		root.set("ZPX", buildZPX(mapper, dinGpPin, quantity, daysSupply, productCost,
				professionalFee, newRefillCode, prescriberId, sigInstructions));
		// ZCD - claim/dispense drug details (mapped from Prescription business data)
		root.set("ZCD", buildZCD(mapper, dinGpPin, quantity, daysSupply, productCost,
				newRefillCode, prescriberId, prescriptionNumber));
		// ZPJ - intervention codes (static defaults + sig instructions)
		root.set("ZPJ",         buildZPJ(mapper, sigInstructions));
		// ZZZ - transaction type metadata (static defaults + prescription number)
		root.set("ZZZ",         buildZZZ(mapper, prescriptionNumber));
		// ZZZSegments - multiple transaction type segments (static defaults)
		root.set("ZZZSegments", buildZZZSegments(mapper, prescriptionNumber));

		return mapper.writeValueAsString(root);
	}

	// ---- private segment builders ----

	private static ObjectNode buildHeader(ObjectMapper m, String messageId, EventConfig cfg, String datetime) {
		ObjectNode n = m.createObjectNode();
		n.put("messageIdRoot", "2.16.840.1.113883.3.51.1.1.1");
		n.put("messageId", messageId);
		n.put("creationTime", "20250604092800");
		n.put("versionCode", cfg.hl7Version());
		n.put("processingCode", "T");
		n.put("processingModeCode", "T");
		n.put("acceptAckCode", "AL");
		n.put("interactionId", cfg.interactionId());
		n.put("interactionIdRoot", "2.16.840.1.113883.3.51.1.1.2");
		return n;
	}

	private static ObjectNode buildSender(ObjectMapper m) {
		ObjectNode n = m.createObjectNode();
		n.put("deviceExtension", "SO_RX");
		n.put("organizationExtension", "SO");
		n.put("senderIdRoot", "2.16.840.1.113883.3.51.1.1.5");
		n.put("organizationIdRoot", "2.16.840.1.113883.3.51.1.1.3");
		return n;
	}

	private static ObjectNode buildReceiver(ObjectMapper m) {
		ObjectNode n = m.createObjectNode();
		n.put("deviceExtension", "BCHCIM");
		n.put("organizationExtension", "HCIM");
		n.put("receiverIdRoot", "2.16.840.1.113883.3.51.1.1.4");
		n.put("organizationIdRoot", "2.16.840.1.113883.3.51.1.1.3");
		return n;
	}

	private static ObjectNode buildMSH(ObjectMapper m, String datetime) {
		ObjectNode n = m.createObjectNode();
		n.put("fieldSeparator", "|");
		n.put("encodingCharacters", "^~\\E\\&");
		n.put("sendingApplication", "SO");
		n.put("sendingFacility", "BC00000F97");
		n.put("receivingApplication", "PNP");
		n.put("receivingFacility", "ERXPP");
		n.put("security", "6LFJ9.*2FIS,RQCB.@ZU:34.152.37.242");
		n.put("messageType", "ZPN");
		n.put("messageControlId", "00000F97250618365111");
		n.put("processingId", "P");
		n.put("versionId", "2.1");
		n.put("dateTimeOfMessage", datetime);
		return n;
	}

	private static ObjectNode buildCandidate(ObjectMapper m, JsonNode patient, String phn) {
		ObjectNode n = m.createObjectNode();
		n.put("personIdExtension", "PSUSH@BC000001CT");
		n.put("birthDate", "19970303");
		n.put("lastName", patient.path("lastName").asText("MASK"));
		n.put("firstName", patient.path("firstName").asText("GENDER"));
		n.put("phn", phn);
		n.put("dob", patient.path("dob").asText("19800101"));
		n.put("gender", patient.path("gender").asText("M"));

		ObjectNode addr = m.createObjectNode();
		JsonNode patAddr = patient.path("address");
		addr.put("line", patAddr.path("line").asText("123 Main St"));
		addr.put("city", patAddr.path("city").asText("Vancouver"));
		addr.put("province", patAddr.path("province").asText("BC"));
		addr.put("postalCode", patAddr.path("postalCode").asText("V5K0A1"));
		n.set("address", addr);

		n.put("phone", patient.path("phone").asText("6045551234"));
		n.put("personIdRoot", "2.16.840.1.113883.3.51.1.1.6.1");
		n.put("personIdValue", "9872205077");
		n.put("dataEntererRoot", "2.16.840.1.113883.3.51.1.1.7");
		return n;
	}

	private static ObjectNode buildEvent(ObjectMapper m, EventConfig cfg, String datetime) {
		ObjectNode n = m.createObjectNode();
		n.put("eventTypeCode", cfg.eventTypeCode());
		n.put("recordedDateTime", datetime);
		return n;
	}

	private static ObjectNode buildVisit(ObjectMapper m, String datetime) {
		ObjectNode n = m.createObjectNode();
		n.put("patientClass", "O");
		n.put("location", "ER");
		n.put("admitDateTime", datetime);
		n.put("providerId", "123456");
		n.put("providerName", "Smith^Jane");
		return n;
	}

	private static ObjectNode buildZCA(ObjectMapper m) {
		ObjectNode n = m.createObjectNode();
		n.put("reserved", "");
		n.put("binNumber", "70");
		n.put("carrierId", "X1");
		n.put("versionNumber", "SD");
		n.put("transactionCode", "02");
		n.put("cardholderIdCode", "");
		n.put("groupNumber", "");
		n.put("clientCode", "");
		n.put("softwareId", "");
		n.put("softwareVersion", "");
		n.put("recordType", "1");
		n.put("carrierIdFromHL7", "01");
		return n;
	}

	private static ObjectNode buildZCB(ObjectMapper m) {
		ObjectNode n = m.createObjectNode();
		n.put("locationId", "BC00000F97");
		n.put("transactionDate", "250618");
		n.put("transactionTime", "");
		n.put("languageCode", "");
		n.put("traceNumber", "365111");
		n.put("reserved6", "");
		n.put("reserved7", "");
		n.put("reserved8", "");
		n.put("reserved9", "");
		n.put("reserved10", "");
		n.put("reserved11", "");
		n.put("reserved12", "");
		n.put("traceNumberRepeated", "365111");
		n.put("locationIdHL7", "BC00000F97");
		n.put("transactionDateHL7", "250618");
		n.put("traceNumberHL7", "365111");
		return n;
	}

	private static ObjectNode buildZCC(ObjectMapper m, String phn) {
		String paddedPhn = "000" + phn;
		ObjectNode n = m.createObjectNode();
		n.put("reserved1", "");
		n.put("reserved2", "");
		n.put("reserved3", "");
		n.put("reserved4", "");
		n.put("reserved5", "");
		n.put("reserved6", "");
		n.put("reserved7", "");
		n.put("reserved8", "");
		n.put("reserved9", "");
		n.put("phn", paddedPhn);
		n.put("phnHL7", paddedPhn);
		return n;
	}

	private static ObjectNode buildZPX(ObjectMapper m, String dinGpPin, String quantity,
			String daysSupply, String productCost, String professionalFee,
			String newRefillCode, String prescriberId, String sigInstructions) {
		ObjectNode n = m.createObjectNode();
		n.put("internalRxId", "ZPX1");
		n.put("newRefillCode", newRefillCode);
		n.put("productSelectionCode", "1");
		n.put("dinGpPin", dinGpPin);
		n.put("reserved5", "");
		n.put("reserved6", "");
		n.put("reserved7", "");
		n.put("reserved8", "");
		n.put("reserved9", "");
		n.put("reserved10", "");
		n.put("reserved11", "");
		n.put("reserved12", "");
		n.put("sigCode", "PHARMACY ASSISTANT");
		n.put("quantity", quantity);
		n.put("daysSupply", daysSupply);
		n.put("reserved16", "");
		n.put("productCost", productCost);
		n.put("reserved18", "");
		n.put("professionalFee", professionalFee);
		n.put("frequencyIntervention", "OTHERFREQUENCY");
		n.put("reserved21", "");
		n.put("reserved22", "");
		n.put("reserved23", "");
		n.put("reserved24", "");
		n.put("reserved25", "");
		n.put("reserved26", "");
		n.put("reserved27", "");
		n.put("reserved28", "");
		n.put("sigInstructions", sigInstructions);
		n.put("reserved30", "");
		n.put("reserved31", "");
		n.put("reserved32", "");
		n.put("reserved33", "");
		n.put("reserved34", "");
		n.put("reserved35", "");
		n.put("prescriberId", prescriberId);
		return n;
	}

	private static ObjectNode buildZCD(ObjectMapper m, String dinGpPin, String quantity,
			String daysSupply, String productCost, String newRefillCode,
			String prescriberId, String prescriptionNumber) {
		ObjectNode n = m.createObjectNode();
		n.put("newRefillCode", newRefillCode);
		n.put("din", "21299844");
		n.put("gpPin", dinGpPin);
		n.put("quantity", quantity);
		n.put("daysSupply", daysSupply);
		n.put("cost", productCost);
		n.put("productId", "33476LO");
		n.put("repeatCount", "3");
		n.put("pharmacyId", "9615");
		n.put("patientId", "1335");
		n.put("providerId", "1160");
		n.put("prescriptionNumber", prescriptionNumber);
		n.put("prescriberId", prescriberId);
		return n;
	}

	private static ArrayNode buildZPJ(ObjectMapper m, String sigInstructions) {
		ArrayNode arr = m.createArrayNode();
		arr.add(m.createObjectNode().put("id", "ZPJ1"));
		arr.add(m.createObjectNode().put("id", "ZPJ2"));
		arr.add(m.createObjectNode().put("id", "ZPJ2"));
		arr.add(m.createObjectNode().put("id", "ZPJ2"));
		arr.add(m.createObjectNode().put("id", "ZPJ3"));
		ObjectNode zpj4 = m.createObjectNode();
		zpj4.put("id", "ZPJ4");
		zpj4.put("instructions", sigInstructions);
		arr.add(zpj4);
		return arr;
	}

	private static ObjectNode buildZZZ(ObjectMapper m, String prescriptionNumber) {
		ObjectNode n = m.createObjectNode();
		n.put("transactionType", "TDU");
		n.put("messageStatusCode", "");
		n.put("reserved3", "");
		n.put("traceNumber", "365111");
		n.put("pharmacyId", "P1");
		n.put("prescriptionNumber", prescriptionNumber);
		n.put("additionalReference", "");
		n.put("rejectionCodes", "");
		n.put("reserved9", "");
		n.put("reserved10", "");
		return n;
	}

	private static ArrayNode buildZZZSegments(ObjectMapper m, String prescriptionNumber) {
		ArrayNode arr = m.createArrayNode();
		ObjectNode seg1 = m.createObjectNode();
		seg1.put("transactionType", "TDU");
		seg1.put("traceNumber", "365111");
		seg1.put("pharmacyId", "P1");
		seg1.put("prescriptionNumber", prescriptionNumber);
		arr.add(seg1);
		ObjectNode seg2 = m.createObjectNode();
		seg2.put("transactionType", "TAC");
		seg2.put("traceNumber", "365111");
		seg2.put("pharmacyId", "P1");
		seg2.put("prescriptionNumber", prescriptionNumber);
		arr.add(seg2);
		return arr;
	}

	// ---- shared utilities ----

	public static String generateRandomId() {
		int length = 20;
		String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
		SecureRandom random = new SecureRandom();
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			sb.append(chars.charAt(random.nextInt(chars.length())));
		}
		return sb.toString();
	}

	public static String formatCurrentDateTime() {
		return ZonedDateTime.now(ZoneId.systemDefault()).format(DATE_FORMATTER);
	}
}
