package com.solis.adj.client.util;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Builds the full NL Pharmacy Network (NLPN) JSON payload from
 * simplified business data entered in the test-client UI, merging
 * in the CeRx infrastructure defaults (Header, Sender, Receiver)
 * that the data-service / enrichment-service expect for
 * {@code COMT_IN300001CA} (Add Patient Note) and future NL
 * messages.
 *
 * <p>Mirrors the BC pattern established in
 * {@link BCPayloadBuilder}: {@link #EVENT_CONFIG} is keyed by the
 * Thymeleaf view name and drives HL7 interaction routing in the
 * downstream enrichment service.</p>
 */
public final class NLPayloadBuilder {

	private static final DateTimeFormatter TIMESTAMP_FORMATTER =
			DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

	public record EventConfig(String interactionId, String versionCode, String province) {}

	public static final Map<String, EventConfig> EVENT_CONFIG = Map.ofEntries(
		Map.entry("nl-add-patient-note",       new EventConfig("COMT_IN300001CA", "V01R04.3", "NL")),
		Map.entry("nl-deprecate-patient-note", new EventConfig("COMT_IN300101CA", "V01R04.3", "NL")),
		Map.entry("nl-patient-note-query",     new EventConfig("COMT_IN300201CA", "V01R04.3", "NL")),
		Map.entry("nl-add-note-to-record",     new EventConfig("COMT_IN301001CA", "V01R04.3", "NL")),
		Map.entry("nl-retract-action",         new EventConfig("COMT_IN600001CA", "V01R04.3", "NL")),
		Map.entry("nl-issue-mgmt-override",    new EventConfig("PORX_IN050001CA", "V01R04.3", "NL")),
		Map.entry("nl-refusal-to-dispense",    new EventConfig("PORX_IN090001CA", "V01R04.3", "NL")),
		Map.entry("nl-dispense-transfer",      new EventConfig("PORX_IN110001CA", "V01R04.3", "NL")),
		Map.entry("nl-activate-rx",            new EventConfig("PORX_IN010380CA", "V01R04.3", "NL")),
		Map.entry("nl-activate-device-rx",     new EventConfig("PORX_IN010300CA", "V01R04.3", "NL")),
		Map.entry("nl-suspend-rx",             new EventConfig("PORX_IN010440CA", "V01R04.3", "NL")),
		Map.entry("nl-resume-rx",              new EventConfig("PORX_IN010520CA", "V01R04.3", "NL")),
		Map.entry("nl-abort-dispense-auth",    new EventConfig("PORX_IN010560CA", "V01R04.3", "NL")),
		Map.entry("nl-abort-rx",               new EventConfig("PORX_IN010840CA", "V01R04.3", "NL")),
		Map.entry("nl-record-dispense",        new EventConfig("PORX_IN020190CA", "V01R04.3", "NL")),
		Map.entry("nl-record-device-dispense", new EventConfig("PORX_IN020060CA", "V01R04.3", "NL")),
		Map.entry("nl-record-pickup",            new EventConfig("PORX_IN020080CA", "V01R04.3", "NL")),
		Map.entry("nl-record-supply-event",      new EventConfig("PORX_IN020210CA", "V01R04.3", "NL")),
		Map.entry("nl-record-dispense-reversal", new EventConfig("PORX_IN020370CA", "V01R04.3", "NL")),
		// ── Section 14: Other Medication / OTC ──
		Map.entry("nl-record-other-med",         new EventConfig("PORX_IN040020CA", "V01R04.3", "NL")),
		Map.entry("nl-update-other-med",         new EventConfig("PORX_IN040070CA", "V01R04.3", "NL")),
		// ── Section 16: Consent ──
		Map.entry("nl-record-consent",           new EventConfig("RCMR_IN010003CA", "V01R04.3", "NL")),
		// ── Section 17: Adverse Reactions ──
		Map.entry("nl-record-adverse-reaction",  new EventConfig("REPC_IN000004CA", "V01R04.3", "NL")),
		Map.entry("nl-update-adverse-reaction",  new EventConfig("REPC_IN000008CA", "V01R04.3", "NL")),
		// ── Section 18: Allergy / Intolerance ──
		Map.entry("nl-add-allergy",              new EventConfig("REPC_IN000012CA", "V01R04.3", "NL")),
		Map.entry("nl-update-allergy",           new EventConfig("REPC_IN000020CA", "V01R04.3", "NL")),
		// ── Section 19: Medical Conditions ──
		Map.entry("nl-record-medical-condition", new EventConfig("REPC_IN000028CA", "V01R04.3", "NL")),
		Map.entry("nl-update-medical-condition", new EventConfig("REPC_IN000032CA", "V01R04.3", "NL")),
		// ── Section 20: Professional Services ──
		Map.entry("nl-record-prof-service",      new EventConfig("REPC_IN000044CA", "V01R04.3", "NL")),
		// ── Section 21: Basic Observations ──
		Map.entry("nl-record-basic-observation", new EventConfig("REPC_IN000051CA", "V01R04.3", "NL")),
		// ── Section 15: Medication / Device Queries (17 query interactions) ──
		Map.entry("nl-query-dev-disp-detail",     new EventConfig("PORX_IN060050CA", "V01R04.3", "NL")),
		Map.entry("nl-query-dev-disp-summary",    new EventConfig("PORX_IN060070CA", "V01R04.3", "NL")),
		Map.entry("nl-query-dev-rx-detail",       new EventConfig("PORX_IN060090CA", "V01R04.3", "NL")),
		Map.entry("nl-query-dev-disp-by-rx",      new EventConfig("PORX_IN060110CA", "V01R04.3", "NL")),
		Map.entry("nl-query-dev-rx-summary",      new EventConfig("PORX_IN060130CA", "V01R04.3", "NL")),
		Map.entry("nl-query-med-rx-detail",       new EventConfig("PORX_IN060170CA", "V01R04.3", "NL")),
		Map.entry("nl-query-med-disp-detail",     new EventConfig("PORX_IN060210CA", "V01R04.3", "NL")),
		Map.entry("nl-query-med-disp-summary",    new EventConfig("PORX_IN060230CA", "V01R04.3", "NL")),
		Map.entry("nl-query-med-rx-detail-rx",    new EventConfig("PORX_IN060250CA", "V01R04.3", "NL")),
		Map.entry("nl-query-med-disp-by-rx",      new EventConfig("PORX_IN060270CA", "V01R04.3", "NL")),
		Map.entry("nl-query-med-rx-summary",      new EventConfig("PORX_IN060290CA", "V01R04.3", "NL")),
		Map.entry("nl-query-med-profile-generic", new EventConfig("PORX_IN060350CA", "V01R04.3", "NL")),
		Map.entry("nl-query-med-profile-detail",  new EventConfig("PORX_IN060370CA", "V01R04.3", "NL")),
		Map.entry("nl-query-med-profile-summary", new EventConfig("PORX_IN060390CA", "V01R04.3", "NL")),
		Map.entry("nl-query-other-med",           new EventConfig("PORX_IN060450CA", "V01R04.3", "NL")),
		Map.entry("nl-query-remaining-fill",      new EventConfig("PORX_IN060470CA", "V01R04.3", "NL")),
		Map.entry("nl-query-unfilled-rx",         new EventConfig("PORX_IN060490CA", "V01R04.3", "NL")),
		// ── Section 17/18/19/20/21 Queries (clinical observation queries) ──
		Map.entry("nl-query-adverse-reactions",   new EventConfig("REPC_IN000001CA", "V01R04.3", "NL")),
		Map.entry("nl-query-allergies",           new EventConfig("REPC_IN000015CA", "V01R04.3", "NL")),
		Map.entry("nl-query-medical-conditions",  new EventConfig("REPC_IN000023CA", "V01R04.3", "NL")),
		Map.entry("nl-query-condition-history",   new EventConfig("REPC_IN000025CA", "V01R04.3", "NL")),
		Map.entry("nl-query-prof-services",       new EventConfig("REPC_IN000041CA", "V01R04.3", "NL")),
		Map.entry("nl-query-basic-observations",  new EventConfig("REPC_IN000054CA", "V01R04.3", "NL")),
		// ── Section 22: Client Registry (V02R02.0 profile) ──
		Map.entry("nl-cr-find-candidates",        new EventConfig("PRPA_IN101103CA", "V02R02.0", "NL")),
		Map.entry("nl-cr-get-demographics",       new EventConfig("PRPA_IN101101CA", "V02R02.0", "NL")),
		Map.entry("nl-cr-add-person",             new EventConfig("PRPA_IN101201CA", "V02R02.0", "NL")),
		Map.entry("nl-cr-revise-person",          new EventConfig("PRPA_IN101204CA", "V02R02.0", "NL")),
		// ── Section 23: Shared / Polling + NL-Defined (V02R02.0 profile) ──
		Map.entry("nl-poll-request",              new EventConfig("MCCI_IN100001CA", "V02R02.0", "NL")),
		Map.entry("nl-poll-fetch-next",           new EventConfig("MCCI_IN100004CA", "V02R02.0", "NL")),
		Map.entry("nl-poll-exception",            new EventConfig("MCCI_IN100005CA", "V02R02.0", "NL")),
		Map.entry("nl-broadcast-topics",          new EventConfig("NLPN_IN100120CA", "V02R02.0", "NL")),
		Map.entry("nl-broadcast-subscribe",       new EventConfig("NLPN_IN100140CA", "V02R02.0", "NL")),
		Map.entry("nl-update-password",           new EventConfig("NLPN_IN100200CA", "V02R02.0", "NL"))
	);

	private NLPayloadBuilder() {}

	/** Validates a view name against the allowed set, falling back to nl-add-patient-note. */
	public static String sanitizeViewName(String candidate) {
		return EVENT_CONFIG.containsKey(candidate) ? candidate : "nl-add-patient-note";
	}

	/** Returns the simplified business-data JSON shown in the UI textarea for the given view. */
	public static String getBusinessDataDefault(String viewName) {
		return switch (viewName) {
			case "nl-add-patient-note" -> """
					{
					  "Sender": {
					    "ehrUserId": "USER-001",
					    "ehrRole": "Pharmacist",
					    "facilityId": "FAC-0001",
					    "softwareId": "SolisPOS",
					    "softwareVersion": "1.0.0"
					  },
					  "Patient": {
					    "phn": "123456789",
					    "firstName": "JANE",
					    "lastName": "DOE",
					    "dob": "19800101",
					    "gender": "F"
					  },
					  "Location": {
					    "locationId": "PHARM-001",
					    "locationOid": "2.16.840.1.113883.4.277"
					  },
					  "Annotation": {
					    "noteText": "Patient prefers generic medications when available.",
					    "noteType": "PATPREF"
					  }
					}""";
			case "nl-deprecate-patient-note" -> """
					{
					  "Sender": {
					    "ehrUserId": "USER-001",
					    "ehrRole": "Pharmacist",
					    "facilityId": "FAC-0001",
					    "softwareId": "SolisPOS",
					    "softwareVersion": "1.0.0"
					  },
					  "Patient": {
					    "phn": "123456789",
					    "firstName": "JANE",
					    "lastName": "DOE",
					    "dob": "19800101",
					    "gender": "F"
					  },
					  "Location": {
					    "locationId": "PHARM-001",
					    "locationOid": "2.16.840.1.113883.4.277"
					  },
					  "Annotation": {
					    "priorNoteId": "11111111-2222-3333-4444-555555555555",
					    "reasonCode": "ERROR",
					    "reasonText": "Note entered against the wrong patient."
					  }
					}""";
			case "nl-patient-note-query" -> """
					{
					  "Sender": {
					    "ehrUserId": "USER-001",
					    "ehrRole": "Pharmacist",
					    "facilityId": "FAC-0001",
					    "softwareId": "SolisPOS",
					    "softwareVersion": "1.0.0"
					  },
					  "Patient": {
					    "phn": "123456789",
					    "firstName": "JANE",
					    "lastName": "DOE",
					    "dob": "19800101",
					    "gender": "F"
					  },
					  "Location": {
					    "locationId": "PHARM-001",
					    "locationOid": "2.16.840.1.113883.4.277"
					  },
					  "Query": {
					    "noteTypeCode": "",
					    "fromDate": "20250101",
					    "toDate":   "20261231"
					  }
					}""";
			case "nl-add-note-to-record" -> """
					{
					  "Sender": {
					    "ehrUserId": "USER-001",
					    "ehrRole": "Pharmacist",
					    "facilityId": "FAC-0001",
					    "softwareId": "SolisPOS",
					    "softwareVersion": "1.0.0"
					  },
					  "Patient": {
					    "phn": "123456789",
					    "firstName": "JANE",
					    "lastName": "DOE",
					    "dob": "19800101",
					    "gender": "F"
					  },
					  "Location": {
					    "locationId": "PHARM-001",
					    "locationOid": "2.16.840.1.113883.4.277"
					  },
					  "RecordReference": {
					    "targetId": "RX-2026-00042",
					    "targetTypeCode": "RXE"
					  },
					  "Annotation": {
					    "noteText": "Counselled patient on interaction risks for this prescription.",
					    "noteType": "RXCNSL"
					  }
					}""";
			case "nl-retract-action" -> """
					{
					  "Sender": {
					    "ehrUserId": "USER-001",
					    "ehrRole": "Pharmacist",
					    "facilityId": "FAC-0001",
					    "softwareId": "SolisPOS",
					    "softwareVersion": "1.0.0"
					  },
					  "Patient": {
					    "phn": "123456789",
					    "firstName": "JANE",
					    "lastName": "DOE",
					    "dob": "19800101",
					    "gender": "F"
					  },
					  "Location": {
					    "locationId": "PHARM-001",
					    "locationOid": "2.16.840.1.113883.4.277"
					  },
					  "Retract": {
					    "targetEventId": "EVT-TO-RETRACT-0001",
					    "simulate": ""
					  }
					}""";
			case "nl-issue-mgmt-override" -> """
					{
					  "Sender": {
					    "ehrUserId": "USER-001",
					    "ehrRole": "Pharmacist",
					    "facilityId": "FAC-0001",
					    "softwareId": "SolisPOS",
					    "softwareVersion": "1.0.0"
					  },
					  "Patient": {
					    "phn": "123456789",
					    "firstName": "JANE",
					    "lastName": "DOE",
					    "dob": "19800101",
					    "gender": "F"
					  },
					  "Location": {
					    "locationId": "PHARM-001",
					    "locationOid": "2.16.840.1.113883.4.277"
					  },
					  "IssueManagement": {
					    "targetRxId": "RX-2026-00042",
					    "issueCode": "DDI",
					    "issueText": "Drug-drug interaction with current therapy.",
					    "managementCode": "OVERRIDE",
					    "managementText": "Prescriber aware; benefit outweighs risk.",
					    "simulate": ""
					  }
					}""";
			case "nl-refusal-to-dispense" -> """
					{
					  "Sender": {
					    "ehrUserId": "USER-001",
					    "ehrRole": "Pharmacist",
					    "facilityId": "FAC-0001",
					    "softwareId": "SolisPOS",
					    "softwareVersion": "1.0.0"
					  },
					  "Patient": {
					    "phn": "123456789",
					    "firstName": "JANE",
					    "lastName": "DOE",
					    "dob": "19800101",
					    "gender": "F"
					  },
					  "Location": {
					    "locationId": "PHARM-001",
					    "locationOid": "2.16.840.1.113883.4.277"
					  },
					  "Refusal": {
					    "targetRxId": "RX-2026-00042",
					    "refusalReasonCode": "ALGY",
					    "refusalReasonText": "Documented allergy to prescribed medication.",
					    "simulate": ""
					  }
					}""";
			case "nl-dispense-transfer" -> """
					{
					  "Sender": {
					    "ehrUserId": "USER-001",
					    "ehrRole": "Pharmacist",
					    "facilityId": "FAC-0001",
					    "softwareId": "SolisPOS",
					    "softwareVersion": "1.0.0"
					  },
					  "Patient": {
					    "phn": "123456789",
					    "firstName": "JANE",
					    "lastName": "DOE",
					    "dob": "19800101",
					    "gender": "F"
					  },
					  "Location": {
					    "locationId": "PHARM-001",
					    "locationOid": "2.16.840.1.113883.4.277"
					  },
					  "Transfer": {
					    "targetDispenseId": "DSP-2026-00099",
					    "newResponsibleProviderId": "USER-777",
					    "newResponsibleFacilityId": "FAC-0099",
					    "reasonCode": "TRANSFER",
					    "reasonText": "Dispense responsibility transferred to covering pharmacist.",
					    "simulate": ""
					  }
					}""";
			case "nl-activate-rx" -> """
					{
					  "Sender": {
					    "ehrUserId": "USER-001",
					    "ehrRole": "Pharmacist",
					    "facilityId": "FAC-0001",
					    "softwareId": "SolisPOS",
					    "softwareVersion": "1.0.0"
					  },
					  "Patient": {
					    "phn": "123456789",
					    "firstName": "JANE",
					    "lastName": "DOE",
					    "dob": "19800101",
					    "gender": "F"
					  },
					  "Location": {
					    "locationId": "PHARM-001",
					    "locationOid": "2.16.840.1.113883.4.277"
					  },
					  "Prescription": {
					    "rxId": "RX-2026-00101",
					    "din": "02443147",
					    "drugName": "LIPITOR 20 MG TABLET",
					    "quantity": "30",
					    "daysSupply": "30",
					    "directions": "Take 1 tablet by mouth once daily at bedtime.",
					    "refills": "5",
					    "prescriberLicense": "DR-12345",
					    "prescriberOid": "2.16.840.1.113883.4.277",
					    "effectiveTime": "20260407",
					    "simulate": ""
					  }
					}""";
			case "nl-activate-device-rx" -> """
					{
					  "Sender": {
					    "ehrUserId": "USER-001",
					    "ehrRole": "Pharmacist",
					    "facilityId": "FAC-0001",
					    "softwareId": "SolisPOS",
					    "softwareVersion": "1.0.0"
					  },
					  "Patient": {
					    "phn": "123456789",
					    "firstName": "JANE",
					    "lastName": "DOE",
					    "dob": "19800101",
					    "gender": "F"
					  },
					  "Location": {
					    "locationId": "PHARM-001",
					    "locationOid": "2.16.840.1.113883.4.277"
					  },
					  "Prescription": {
					    "rxId": "RX-2026-00102",
					    "deviceCode": "ACC-GLUC-METER",
					    "deviceName": "BLOOD GLUCOSE METER",
					    "deviceQuantity": "1",
					    "directions": "Use as directed for blood glucose self-monitoring.",
					    "refills": "0",
					    "prescriberLicense": "DR-12345",
					    "prescriberOid": "2.16.840.1.113883.4.277",
					    "effectiveTime": "20260407",
					    "simulate": ""
					  }
					}""";
			case "nl-suspend-rx" -> """
					{
					  "Sender": {
					    "ehrUserId": "USER-001",
					    "ehrRole": "Pharmacist",
					    "facilityId": "FAC-0001",
					    "softwareId": "SolisPOS",
					    "softwareVersion": "1.0.0"
					  },
					  "Patient": {
					    "phn": "123456789",
					    "firstName": "JANE",
					    "lastName": "DOE",
					    "dob": "19800101",
					    "gender": "F"
					  },
					  "Location": {
					    "locationId": "PHARM-001",
					    "locationOid": "2.16.840.1.113883.4.277"
					  },
					  "RxLifecycle": {
					    "targetRxId": "RX-2026-00101",
					    "reasonCode": "HOSP",
					    "reasonText": "Patient hospitalised; suspend outpatient therapy until discharge.",
					    "effectiveTime": "20260407",
					    "simulate": ""
					  }
					}""";
			case "nl-resume-rx" -> """
					{
					  "Sender": {
					    "ehrUserId": "USER-001",
					    "ehrRole": "Pharmacist",
					    "facilityId": "FAC-0001",
					    "softwareId": "SolisPOS",
					    "softwareVersion": "1.0.0"
					  },
					  "Patient": {
					    "phn": "123456789",
					    "firstName": "JANE",
					    "lastName": "DOE",
					    "dob": "19800101",
					    "gender": "F"
					  },
					  "Location": {
					    "locationId": "PHARM-001",
					    "locationOid": "2.16.840.1.113883.4.277"
					  },
					  "RxLifecycle": {
					    "targetRxId": "RX-2026-00101",
					    "reasonCode": "DISCHG",
					    "reasonText": "Patient discharged from hospital; resume outpatient therapy.",
					    "effectiveTime": "20260407",
					    "simulate": ""
					  }
					}""";
			case "nl-abort-dispense-auth" -> """
					{
					  "Sender": {
					    "ehrUserId": "USER-001",
					    "ehrRole": "Pharmacist",
					    "facilityId": "FAC-0001",
					    "softwareId": "SolisPOS",
					    "softwareVersion": "1.0.0"
					  },
					  "Patient": {
					    "phn": "123456789",
					    "firstName": "JANE",
					    "lastName": "DOE",
					    "dob": "19800101",
					    "gender": "F"
					  },
					  "Location": {
					    "locationId": "PHARM-001",
					    "locationOid": "2.16.840.1.113883.4.277"
					  },
					  "RxLifecycle": {
					    "targetRxId": "DSP-AUTH-2026-00055",
					    "reasonCode": "PATREQ",
					    "reasonText": "Patient requested cancellation of remaining dispense authorization.",
					    "effectiveTime": "20260407",
					    "simulate": ""
					  }
					}""";
			case "nl-abort-rx" -> """
					{
					  "Sender": {
					    "ehrUserId": "USER-001",
					    "ehrRole": "Pharmacist",
					    "facilityId": "FAC-0001",
					    "softwareId": "SolisPOS",
					    "softwareVersion": "1.0.0"
					  },
					  "Patient": {
					    "phn": "123456789",
					    "firstName": "JANE",
					    "lastName": "DOE",
					    "dob": "19800101",
					    "gender": "F"
					  },
					  "Location": {
					    "locationId": "PHARM-001",
					    "locationOid": "2.16.840.1.113883.4.277"
					  },
					  "RxLifecycle": {
					    "targetRxId": "RX-2026-00101",
					    "reasonCode": "THERCH",
					    "reasonText": "Therapy changed; abort this prescription.",
					    "effectiveTime": "20260407",
					    "simulate": ""
					  }
					}""";
			case "nl-record-dispense" -> """
					{
					  "Sender": {
					    "ehrUserId": "USER-001",
					    "ehrRole": "Pharmacist",
					    "facilityId": "FAC-0001",
					    "softwareId": "SolisPOS",
					    "softwareVersion": "1.0.0"
					  },
					  "Patient": {
					    "phn": "123456789",
					    "firstName": "JANE",
					    "lastName": "DOE",
					    "dob": "19800101",
					    "gender": "F"
					  },
					  "Location": {
					    "locationId": "PHARM-001",
					    "locationOid": "2.16.840.1.113883.4.277"
					  },
					  "Dispense": {
					    "dispenseId": "DSP-2026-00201",
					    "prescriptionId": "RX-2026-00101",
					    "din": "02443147",
					    "drugName": "LIPITOR 20 MG TABLET",
					    "quantity": "30",
					    "daysSupply": "30",
					    "lotNumber": "LOT-0407-A",
					    "expirationDate": "20270407",
					    "dispensedBy": "USER-001",
					    "effectiveTime": "20260407",
					    "simulate": ""
					  }
					}""";
			case "nl-record-device-dispense" -> """
					{
					  "Sender": {
					    "ehrUserId": "USER-001",
					    "ehrRole": "Pharmacist",
					    "facilityId": "FAC-0001",
					    "softwareId": "SolisPOS",
					    "softwareVersion": "1.0.0"
					  },
					  "Patient": {
					    "phn": "123456789",
					    "firstName": "JANE",
					    "lastName": "DOE",
					    "dob": "19800101",
					    "gender": "F"
					  },
					  "Location": {
					    "locationId": "PHARM-001",
					    "locationOid": "2.16.840.1.113883.4.277"
					  },
					  "DeviceDispense": {
					    "dispenseId": "DSP-DEV-2026-00301",
					    "prescriptionId": "RX-2026-00102",
					    "deviceCode": "ACC-GLUC-METER",
					    "deviceName": "BLOOD GLUCOSE METER",
					    "deviceQuantity": "1",
					    "serialNumber": "SN-ABC-123456",
					    "lotNumber": "LOT-DEV-0407",
					    "expirationDate": "20270407",
					    "dispensedBy": "USER-001",
					    "effectiveTime": "20260407",
					    "simulate": ""
					  }
					}""";
			case "nl-record-pickup" -> """
					{
					  "Sender": {
					    "ehrUserId": "USER-001",
					    "ehrRole": "Pharmacist",
					    "facilityId": "FAC-0001",
					    "softwareId": "SolisPOS",
					    "softwareVersion": "1.0.0"
					  },
					  "Patient": {
					    "phn": "123456789",
					    "firstName": "JANE",
					    "lastName": "DOE",
					    "dob": "19800101",
					    "gender": "F"
					  },
					  "Location": {
					    "locationId": "PHARM-001",
					    "locationOid": "2.16.840.1.113883.4.277"
					  },
					  "Pickup": {
					    "dispenseId": "DSP-2026-00201",
					    "pickedUpBy": "PATIENT",
					    "effectiveTime": "20260407",
					    "simulate": ""
					  }
					}""";
			case "nl-record-supply-event" -> """
					{
					  "Sender": {
					    "ehrUserId": "USER-001",
					    "ehrRole": "Pharmacist",
					    "facilityId": "FAC-0001",
					    "softwareId": "SolisPOS",
					    "softwareVersion": "1.0.0"
					  },
					  "Patient": {
					    "phn": "123456789",
					    "firstName": "JANE",
					    "lastName": "DOE",
					    "dob": "19800101",
					    "gender": "F"
					  },
					  "Location": {
					    "locationId": "PHARM-001",
					    "locationOid": "2.16.840.1.113883.4.277"
					  },
					  "SupplyEvent": {
					    "din": "02345678",
					    "drugName": "ACETAMINOPHEN 500 MG TABLET",
					    "quantity": "500",
					    "facilityId": "FAC-WARD-001",
					    "lotNumber": "LOT-WS-0407",
					    "expirationDate": "20270407",
					    "effectiveTime": "20260407",
					    "simulate": ""
					  }
					}""";
			case "nl-record-dispense-reversal" -> """
					{
					  "Sender": {
					    "ehrUserId": "USER-001",
					    "ehrRole": "Pharmacist",
					    "facilityId": "FAC-0001",
					    "softwareId": "SolisPOS",
					    "softwareVersion": "1.0.0"
					  },
					  "Patient": {
					    "phn": "123456789",
					    "firstName": "JANE",
					    "lastName": "DOE",
					    "dob": "19800101",
					    "gender": "F"
					  },
					  "Location": {
					    "locationId": "PHARM-001",
					    "locationOid": "2.16.840.1.113883.4.277"
					  },
					  "DispenseReversal": {
					    "dispenseId": "DSP-2026-00201",
					    "reasonCode": "NOTPICK",
					    "reasonText": "Medication not picked up by patient; reversed.",
					    "effectiveTime": "20260407",
					    "simulate": ""
					  }
					}""";
			case "nl-record-other-med", "nl-update-other-med" -> """
					{
					  "Sender": {
					    "ehrUserId": "USER-001",
					    "ehrRole": "Pharmacist",
					    "facilityId": "FAC-0001",
					    "softwareId": "SolisPOS",
					    "softwareVersion": "1.0.0"
					  },
					  "Patient": {
					    "phn": "123456789",
					    "firstName": "JANE",
					    "lastName": "DOE",
					    "dob": "19800101",
					    "gender": "F"
					  },
					  "Location": {
					    "locationId": "PHARM-001",
					    "locationOid": "2.16.840.1.113883.4.277"
					  },
					  "OtherMedication": {
					    "medicationId": "OTC-2026-00501",
					    "din": "00559407",
					    "gtin": "00612345678901",
					    "description": "IBUPROFEN 200 MG TABLET (OTC)",
					    "quantity": "50",
					    "dosageInstructions": "Take 1-2 tablets every 4-6 hours as needed for pain.",
					    "effectiveTime": "20260407",
					    "simulate": ""
					  }
					}""";
			case "nl-record-consent" -> """
					{
					  "Sender": {
					    "ehrUserId": "USER-001",
					    "ehrRole": "Pharmacist",
					    "facilityId": "FAC-0001",
					    "softwareId": "SolisPOS",
					    "softwareVersion": "1.0.0"
					  },
					  "Patient": {
					    "phn": "123456789",
					    "firstName": "JANE",
					    "lastName": "DOE",
					    "dob": "19800101",
					    "gender": "F"
					  },
					  "Location": {
					    "locationId": "PHARM-001",
					    "locationOid": "2.16.840.1.113883.4.277"
					  },
					  "Consent": {
					    "consentId": "CNS-2026-00101",
					    "patientId": "123456789",
					    "scope": "record",
					    "reason": "Patient has consented to sharing their pharmacy record with the pharmacy team.",
					    "effectiveTime": "20260407",
					    "simulate": ""
					  }
					}""";
			case "nl-record-adverse-reaction", "nl-update-adverse-reaction" -> """
					{
					  "Sender": {
					    "ehrUserId": "USER-001",
					    "ehrRole": "Pharmacist",
					    "facilityId": "FAC-0001",
					    "softwareId": "SolisPOS",
					    "softwareVersion": "1.0.0"
					  },
					  "Patient": {
					    "phn": "123456789",
					    "firstName": "JANE",
					    "lastName": "DOE",
					    "dob": "19800101",
					    "gender": "F"
					  },
					  "Location": {
					    "locationId": "PHARM-001",
					    "locationOid": "2.16.840.1.113883.4.277"
					  },
					  "AdverseReaction": {
					    "reactionId": "AR-2026-00101",
					    "agentCode": "00559407",
					    "agentDescription": "IBUPROFEN",
					    "severity": "moderate",
					    "manifestation": "Generalised urticaria; pruritus on trunk.",
					    "onsetDate": "20260405",
					    "simulate": ""
					  }
					}""";
			case "nl-add-allergy", "nl-update-allergy" -> """
					{
					  "Sender": {
					    "ehrUserId": "USER-001",
					    "ehrRole": "Pharmacist",
					    "facilityId": "FAC-0001",
					    "softwareId": "SolisPOS",
					    "softwareVersion": "1.0.0"
					  },
					  "Patient": {
					    "phn": "123456789",
					    "firstName": "JANE",
					    "lastName": "DOE",
					    "dob": "19800101",
					    "gender": "F"
					  },
					  "Location": {
					    "locationId": "PHARM-001",
					    "locationOid": "2.16.840.1.113883.4.277"
					  },
					  "Allergy": {
					    "allergyId": "ALG-2026-00101",
					    "agentCode": "PENICILLIN",
					    "agentDescription": "Penicillin-class antibiotics",
					    "type": "allergy",
					    "severity": "severe",
					    "manifestation": "Anaphylaxis; documented history of airway involvement.",
					    "onsetDate": "20200315",
					    "simulate": ""
					  }
					}""";
			case "nl-record-medical-condition", "nl-update-medical-condition" -> """
					{
					  "Sender": {
					    "ehrUserId": "USER-001",
					    "ehrRole": "Pharmacist",
					    "facilityId": "FAC-0001",
					    "softwareId": "SolisPOS",
					    "softwareVersion": "1.0.0"
					  },
					  "Patient": {
					    "phn": "123456789",
					    "firstName": "JANE",
					    "lastName": "DOE",
					    "dob": "19800101",
					    "gender": "F"
					  },
					  "Location": {
					    "locationId": "PHARM-001",
					    "locationOid": "2.16.840.1.113883.4.277"
					  },
					  "MedicalCondition": {
					    "conditionId": "COND-2026-00101",
					    "code": "I10",
					    "codeSystem": "ICD-10-CA",
					    "description": "Essential (primary) hypertension.",
					    "onsetDate": "20240201",
					    "status": "active",
					    "simulate": ""
					  }
					}""";
			case "nl-record-prof-service" -> """
					{
					  "Sender": {
					    "ehrUserId": "USER-001",
					    "ehrRole": "Pharmacist",
					    "facilityId": "FAC-0001",
					    "softwareId": "SolisPOS",
					    "softwareVersion": "1.0.0"
					  },
					  "Patient": {
					    "phn": "123456789",
					    "firstName": "JANE",
					    "lastName": "DOE",
					    "dob": "19800101",
					    "gender": "F"
					  },
					  "Location": {
					    "locationId": "PHARM-001",
					    "locationOid": "2.16.840.1.113883.4.277"
					  },
					  "ProfessionalService": {
					    "serviceId": "SVC-2026-00101",
					    "code": "MEDREVW",
					    "description": "Comprehensive medication review performed with the patient.",
					    "effectiveTime": "20260407",
					    "performerId": "USER-001",
					    "simulate": ""
					  }
					}""";
			case "nl-record-basic-observation" -> """
					{
					  "Sender": {
					    "ehrUserId": "USER-001",
					    "ehrRole": "Pharmacist",
					    "facilityId": "FAC-0001",
					    "softwareId": "SolisPOS",
					    "softwareVersion": "1.0.0"
					  },
					  "Patient": {
					    "phn": "123456789",
					    "firstName": "JANE",
					    "lastName": "DOE",
					    "dob": "19800101",
					    "gender": "F"
					  },
					  "Location": {
					    "locationId": "PHARM-001",
					    "locationOid": "2.16.840.1.113883.4.277"
					  },
					  "BasicObservation": {
					    "observationId": "OBS-2026-00101",
					    "code": "8480-6",
					    "codeSystem": "LOINC",
					    "description": "Systolic blood pressure",
					    "value": "128",
					    "unit": "mm[Hg]",
					    "effectiveTime": "202604071430",
					    "simulate": ""
					  }
					}""";
			case "nl-query-dev-disp-detail",
				 "nl-query-dev-disp-summary",
				 "nl-query-dev-rx-detail",
				 "nl-query-dev-disp-by-rx",
				 "nl-query-dev-rx-summary",
				 "nl-query-med-rx-detail",
				 "nl-query-med-disp-detail",
				 "nl-query-med-disp-summary",
				 "nl-query-med-rx-detail-rx",
				 "nl-query-med-disp-by-rx",
				 "nl-query-med-rx-summary",
				 "nl-query-med-profile-generic",
				 "nl-query-med-profile-detail",
				 "nl-query-med-profile-summary",
				 "nl-query-other-med",
				 "nl-query-remaining-fill",
				 "nl-query-unfilled-rx",
				 "nl-query-adverse-reactions",
				 "nl-query-allergies",
				 "nl-query-medical-conditions",
				 "nl-query-condition-history",
				 "nl-query-prof-services",
				 "nl-query-basic-observations" -> buildQueryDefaultJson(viewName);
			case "nl-cr-find-candidates" -> """
					{
					  "Sender": {
					    "ehrUserId": "USER-001",
					    "ehrRole": "Pharmacist",
					    "facilityId": "FAC-0001",
					    "softwareId": "SolisPOS",
					    "softwareVersion": "1.0.0"
					  },
					  "Patient": {
					    "phn": "",
					    "firstName": "JANE",
					    "lastName": "DOE",
					    "dob": "19800101",
					    "gender": "F"
					  },
					  "Location": {
					    "locationId": "PHARM-001",
					    "locationOid": "2.16.840.1.113883.4.277"
					  },
					  "Query": {
					    "queryId": "CR-FIND-2026-00001",
					    "firstName": "JANE",
					    "lastName": "DOE",
					    "dob": "19800101",
					    "gender": "F",
					    "simulate": ""
					  }
					}""";
			case "nl-cr-get-demographics" -> """
					{
					  "Sender": {
					    "ehrUserId": "USER-001",
					    "ehrRole": "Pharmacist",
					    "facilityId": "FAC-0001",
					    "softwareId": "SolisPOS",
					    "softwareVersion": "1.0.0"
					  },
					  "Patient": {
					    "phn": "123456789"
					  },
					  "Location": {
					    "locationId": "PHARM-001",
					    "locationOid": "2.16.840.1.113883.4.277"
					  },
					  "Query": {
					    "queryId": "CR-GETDEM-2026-00001",
					    "simulate": ""
					  }
					}""";
			case "nl-cr-add-person" -> """
					{
					  "Sender": {
					    "ehrUserId": "USER-001",
					    "ehrRole": "Pharmacist",
					    "facilityId": "FAC-0001",
					    "softwareId": "SolisPOS",
					    "softwareVersion": "1.0.0"
					  },
					  "Patient": {
					    "phn": "987654321",
					    "firstName": "JOHN",
					    "lastName": "SMITH",
					    "dob": "19750601",
					    "gender": "M",
					    "addressLine": "1 Water Street",
					    "city": "St. John's",
					    "province": "NL",
					    "postalCode": "A1C1A1",
					    "phone": "(709) 555-0100",
					    "simulate": ""
					  },
					  "Location": {
					    "locationId": "PHARM-001",
					    "locationOid": "2.16.840.1.113883.4.277"
					  }
					}""";
			case "nl-cr-revise-person" -> """
					{
					  "Sender": {
					    "ehrUserId": "USER-001",
					    "ehrRole": "Pharmacist",
					    "facilityId": "FAC-0001",
					    "softwareId": "SolisPOS",
					    "softwareVersion": "1.0.0"
					  },
					  "Patient": {
					    "phn": "987654321",
					    "firstName": "JOHN",
					    "lastName": "SMITH",
					    "dob": "19750601",
					    "gender": "M",
					    "addressLine": "200 Main Road",
					    "city": "Mount Pearl",
					    "province": "NL",
					    "postalCode": "A1N4S6",
					    "phone": "(709) 555-0190",
					    "simulate": ""
					  },
					  "Location": {
					    "locationId": "PHARM-001",
					    "locationOid": "2.16.840.1.113883.4.277"
					  }
					}""";
			case "nl-poll-request", "nl-poll-fetch-next" -> """
					{
					  "Sender": {
					    "ehrUserId": "USER-001",
					    "ehrRole": "Pharmacist",
					    "facilityId": "FAC-0001",
					    "softwareId": "SolisPOS",
					    "softwareVersion": "1.0.0"
					  },
					  "Location": {
					    "locationId": "PHARM-001",
					    "locationOid": "2.16.840.1.113883.4.277"
					  },
					  "Polling": {
					    "queueId": "FAC-0001-INBOX",
					    "simulate": ""
					  }
					}""";
			case "nl-poll-exception" -> """
					{
					  "Sender": {
					    "ehrUserId": "USER-001",
					    "ehrRole": "Pharmacist",
					    "facilityId": "FAC-0001",
					    "softwareId": "SolisPOS",
					    "softwareVersion": "1.0.0"
					  },
					  "Location": {
					    "locationId": "PHARM-001",
					    "locationOid": "2.16.840.1.113883.4.277"
					  },
					  "Polling": {
					    "queueId": "FAC-0001-INBOX",
					    "exceptionCode": "POLL_PAYLOAD_INVALID",
					    "exceptionText": "Polled message payload could not be deserialised by the POS.",
					    "simulate": ""
					  }
					}""";
			case "nl-broadcast-topics" -> """
					{
					  "Sender": {
					    "ehrUserId": "USER-001",
					    "ehrRole": "Pharmacist",
					    "facilityId": "FAC-0001",
					    "softwareId": "SolisPOS",
					    "softwareVersion": "1.0.0"
					  },
					  "Location": {
					    "locationId": "PHARM-001",
					    "locationOid": "2.16.840.1.113883.4.277"
					  },
					  "Query": {
					    "queryId": "BCAST-TOPICS-2026-00001",
					    "topicCategory": "",
					    "simulate": ""
					  }
					}""";
			case "nl-broadcast-subscribe" -> """
					{
					  "Sender": {
					    "ehrUserId": "USER-001",
					    "ehrRole": "Pharmacist",
					    "facilityId": "FAC-0001",
					    "softwareId": "SolisPOS",
					    "softwareVersion": "1.0.0"
					  },
					  "Location": {
					    "locationId": "PHARM-001",
					    "locationOid": "2.16.840.1.113883.4.277"
					  },
					  "Subscribe": {
					    "subscriptionId": "SUB-2026-00001",
					    "topicId": "TOPIC-OUTAGE",
					    "topicName": "System Outage Notifications",
					    "simulate": ""
					  }
					}""";
			case "nl-update-password" -> """
					{
					  "Sender": {
					    "ehrUserId": "USER-001",
					    "ehrRole": "Pharmacist",
					    "facilityId": "FAC-0001",
					    "softwareId": "SolisPOS",
					    "softwareVersion": "1.0.0"
					  },
					  "Location": {
					    "locationId": "PHARM-001",
					    "locationOid": "2.16.840.1.113883.4.277"
					  },
					  "Password": {
					    "currentPasswordHash": "OLDPWHASH-DO-NOT-LOG",
					    "newPasswordHash": "NEWPWHASH-DO-NOT-LOG",
					    "simulate": ""
					  }
					}""";
			default -> "{}";
		};
	}

	/**
	 * Returns the default UI textarea JSON for an NLPN query view (Section 15
	 * medication/device queries plus the clinical observation query siblings of
	 * Sections 17-21). Tailors the {@code Query} node fields based on the view
	 * (dispense id, rx id, din, prescriber license, etc.) so the user sees a
	 * clinically meaningful default while keeping all queries on a uniform
	 * payload shape.
	 *
	 * <p>Supported {@code Query.simulate} values: {@code ""} -> default response
	 * with one sample subject; {@code "empty"} -> empty result set with
	 * {@code statusCode="NF"}; {@code "refused"} -> generic
	 * {@code MCCI_IN000003CA} application reject.</p>
	 */
	private static String buildQueryDefaultJson(String viewName) {
		String querySpecific = switch (viewName) {
			case "nl-query-dev-disp-detail" -> """
					    "dispenseId": "DSP-DEV-2026-00301",""";
			case "nl-query-med-disp-detail" -> """
					    "dispenseId": "DSP-2026-00201",""";
			case "nl-query-dev-rx-detail",
				 "nl-query-dev-disp-by-rx" -> """
					    "rxId": "RX-2026-00102",""";
			case "nl-query-med-rx-detail",
				 "nl-query-med-rx-detail-rx",
				 "nl-query-med-disp-by-rx" -> """
					    "rxId": "RX-2026-00101",""";
			case "nl-query-other-med" -> """
					    "din": "00559407",""";
			case "nl-query-remaining-fill",
				 "nl-query-unfilled-rx" -> """
					    "prescriberLicense": "DR-12345",""";
			default -> "";
		};
		String specificLine = querySpecific.isEmpty() ? "" : "\n" + querySpecific;
		return """
				{
				  "Sender": {
				    "ehrUserId": "USER-001",
				    "ehrRole": "Pharmacist",
				    "facilityId": "FAC-0001",
				    "softwareId": "SolisPOS",
				    "softwareVersion": "1.0.0"
				  },
				  "Patient": {
				    "phn": "123456789",
				    "firstName": "JANE",
				    "lastName": "DOE",
				    "dob": "19800101",
				    "gender": "F"
				  },
				  "Location": {
				    "locationId": "PHARM-001",
				    "locationOid": "2.16.840.1.113883.4.277"
				  },
				  "Query": {
				    "queryId": "QRY-2026-00001",%s
				    "fromDate": "20250101",
				    "toDate": "20261231",
				    "simulate": ""
				  }
				}""".formatted(specificLine);
	}

	/**
	 * Merges simplified business JSON with CeRx infrastructure defaults
	 * to produce the full payload expected by the NL data-service endpoint.
	 */
	public static String buildFullPayload(String businessJson, String viewName) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode bizNode = mapper.readTree(businessJson);

		EventConfig config = EVENT_CONFIG.get(viewName);
		String creationTime = formatCurrentTimestamp();
		String messageId = UUID.randomUUID().toString();

		ObjectNode root = mapper.createObjectNode();

		root.set("Header",   buildHeader(mapper, messageId, config, creationTime));
		root.set("Sender",   copyOrDefault(mapper, bizNode.path("Sender"),   buildDefaultSender(mapper)));
		root.set("Receiver", buildReceiver(mapper));
		root.set("Patient",  copyOrDefault(mapper, bizNode.path("Patient"),  buildDefaultPatient(mapper)));
		root.set("Location", copyOrDefault(mapper, bizNode.path("Location"), buildDefaultLocation(mapper)));

		// Message-specific sections - only included when relevant to the interaction.
		String interactionId = config.interactionId();
		switch (interactionId) {
			case "COMT_IN300001CA" ->
				root.set("Annotation", copyOrDefault(mapper, bizNode.path("Annotation"), buildDefaultAnnotation(mapper)));
			case "COMT_IN300101CA" ->
				root.set("Annotation", copyOrDefault(mapper, bizNode.path("Annotation"), buildDefaultDeprecateAnnotation(mapper)));
			case "COMT_IN300201CA" ->
				root.set("Query", copyOrDefault(mapper, bizNode.path("Query"), buildDefaultQuery(mapper)));
			case "COMT_IN301001CA" -> {
				root.set("RecordReference", copyOrDefault(mapper, bizNode.path("RecordReference"), buildDefaultRecordReference(mapper)));
				root.set("Annotation", copyOrDefault(mapper, bizNode.path("Annotation"), buildDefaultRecordAnnotation(mapper)));
			}
			case "COMT_IN600001CA" ->
				root.set("Retract", copyOrDefault(mapper, bizNode.path("Retract"), buildDefaultRetract(mapper)));
			case "PORX_IN050001CA" ->
				root.set("IssueManagement", copyOrDefault(mapper, bizNode.path("IssueManagement"), buildDefaultIssueManagement(mapper)));
			case "PORX_IN090001CA" ->
				root.set("Refusal", copyOrDefault(mapper, bizNode.path("Refusal"), buildDefaultRefusal(mapper)));
			case "PORX_IN110001CA" ->
				root.set("Transfer", copyOrDefault(mapper, bizNode.path("Transfer"), buildDefaultTransfer(mapper)));
			case "PORX_IN010380CA" ->
				root.set("Prescription", copyOrDefault(mapper, bizNode.path("Prescription"), buildDefaultRxCreate(mapper)));
			case "PORX_IN010300CA" ->
				root.set("Prescription", copyOrDefault(mapper, bizNode.path("Prescription"), buildDefaultRxDeviceCreate(mapper)));
			case "PORX_IN010440CA", "PORX_IN010520CA", "PORX_IN010560CA", "PORX_IN010840CA" ->
				root.set("RxLifecycle", copyOrDefault(mapper, bizNode.path("RxLifecycle"), buildDefaultRxLifecycle(mapper, interactionId)));
			case "PORX_IN020190CA" ->
				root.set("Dispense", copyOrDefault(mapper, bizNode.path("Dispense"), buildDefaultDispense(mapper)));
			case "PORX_IN020060CA" ->
				root.set("DeviceDispense", copyOrDefault(mapper, bizNode.path("DeviceDispense"), buildDefaultDeviceDispense(mapper)));
			case "PORX_IN020080CA" ->
				root.set("Pickup", copyOrDefault(mapper, bizNode.path("Pickup"), buildDefaultPickup(mapper)));
			case "PORX_IN020210CA" ->
				root.set("SupplyEvent", copyOrDefault(mapper, bizNode.path("SupplyEvent"), buildDefaultSupplyEvent(mapper)));
			case "PORX_IN020370CA" ->
				root.set("DispenseReversal", copyOrDefault(mapper, bizNode.path("DispenseReversal"), buildDefaultDispenseReversal(mapper)));
			case "PORX_IN040020CA", "PORX_IN040070CA" ->
				root.set("OtherMedication", copyOrDefault(mapper, bizNode.path("OtherMedication"), buildDefaultOtherMedication(mapper)));
			case "RCMR_IN010003CA" ->
				root.set("Consent", copyOrDefault(mapper, bizNode.path("Consent"), buildDefaultConsent(mapper)));
			case "REPC_IN000004CA", "REPC_IN000008CA" ->
				root.set("AdverseReaction", copyOrDefault(mapper, bizNode.path("AdverseReaction"), buildDefaultAdverseReaction(mapper)));
			case "REPC_IN000012CA", "REPC_IN000020CA" ->
				root.set("Allergy", copyOrDefault(mapper, bizNode.path("Allergy"), buildDefaultAllergy(mapper)));
			case "REPC_IN000028CA", "REPC_IN000032CA" ->
				root.set("MedicalCondition", copyOrDefault(mapper, bizNode.path("MedicalCondition"), buildDefaultMedicalCondition(mapper)));
			case "REPC_IN000044CA" ->
				root.set("ProfessionalService", copyOrDefault(mapper, bizNode.path("ProfessionalService"), buildDefaultProfessionalService(mapper)));
			case "REPC_IN000051CA" ->
				root.set("BasicObservation", copyOrDefault(mapper, bizNode.path("BasicObservation"), buildDefaultBasicObservation(mapper)));
			case "PORX_IN060050CA", "PORX_IN060070CA", "PORX_IN060090CA", "PORX_IN060110CA",
				 "PORX_IN060130CA", "PORX_IN060170CA", "PORX_IN060210CA", "PORX_IN060230CA",
				 "PORX_IN060250CA", "PORX_IN060270CA", "PORX_IN060290CA", "PORX_IN060350CA",
				 "PORX_IN060370CA", "PORX_IN060390CA", "PORX_IN060450CA", "PORX_IN060470CA",
				 "PORX_IN060490CA", "REPC_IN000001CA", "REPC_IN000015CA", "REPC_IN000023CA",
				 "REPC_IN000025CA", "REPC_IN000041CA", "REPC_IN000054CA" ->
				root.set("Query", copyOrDefault(mapper, bizNode.path("Query"), buildDefaultNlpnQuery(mapper, interactionId)));
			// ── Section 22: Client Registry queries ──
			case "PRPA_IN101103CA" ->
				root.set("Query", copyOrDefault(mapper, bizNode.path("Query"), buildDefaultCrFindCandidatesQuery(mapper)));
			case "PRPA_IN101101CA" ->
				root.set("Query", copyOrDefault(mapper, bizNode.path("Query"), buildDefaultCrGetDemographicsQuery(mapper)));
			// ── Section 22: Client Registry writes (Add / Revise) - the Patient node IS the payload ──
			case "PRPA_IN101201CA", "PRPA_IN101204CA" -> {
				// Add/Revise carry only Patient + Location (already merged above) plus a simulate flag.
				// No additional business node required.
			}
			// ── Section 23: Polling control messages ──
			case "MCCI_IN100001CA", "MCCI_IN100004CA" ->
				root.set("Polling", copyOrDefault(mapper, bizNode.path("Polling"), buildDefaultPollingRequest(mapper)));
			case "MCCI_IN100005CA" ->
				root.set("Polling", copyOrDefault(mapper, bizNode.path("Polling"), buildDefaultPollingException(mapper)));
			// ── Section 23: NL-Defined ──
			case "NLPN_IN100120CA" ->
				root.set("Query", copyOrDefault(mapper, bizNode.path("Query"), buildDefaultBroadcastTopicsQuery(mapper)));
			case "NLPN_IN100140CA" ->
				root.set("Subscribe", copyOrDefault(mapper, bizNode.path("Subscribe"), buildDefaultBroadcastSubscribe(mapper)));
			case "NLPN_IN100200CA" ->
				root.set("Password", copyOrDefault(mapper, bizNode.path("Password"), buildDefaultPasswordUpdate(mapper)));
			default -> {
				// Unknown interaction - copy whatever the caller supplied verbatim for forward-compat.
				if (bizNode.path("Annotation").isObject())         root.set("Annotation", bizNode.path("Annotation").deepCopy());
				if (bizNode.path("Query").isObject())              root.set("Query", bizNode.path("Query").deepCopy());
				if (bizNode.path("RecordReference").isObject())    root.set("RecordReference", bizNode.path("RecordReference").deepCopy());
				if (bizNode.path("Retract").isObject())            root.set("Retract", bizNode.path("Retract").deepCopy());
				if (bizNode.path("IssueManagement").isObject())    root.set("IssueManagement", bizNode.path("IssueManagement").deepCopy());
				if (bizNode.path("Refusal").isObject())            root.set("Refusal", bizNode.path("Refusal").deepCopy());
				if (bizNode.path("Transfer").isObject())           root.set("Transfer", bizNode.path("Transfer").deepCopy());
				if (bizNode.path("Prescription").isObject())       root.set("Prescription", bizNode.path("Prescription").deepCopy());
				if (bizNode.path("RxLifecycle").isObject())        root.set("RxLifecycle", bizNode.path("RxLifecycle").deepCopy());
				if (bizNode.path("Dispense").isObject())           root.set("Dispense", bizNode.path("Dispense").deepCopy());
				if (bizNode.path("DeviceDispense").isObject())     root.set("DeviceDispense", bizNode.path("DeviceDispense").deepCopy());
				if (bizNode.path("Pickup").isObject())             root.set("Pickup", bizNode.path("Pickup").deepCopy());
				if (bizNode.path("SupplyEvent").isObject())        root.set("SupplyEvent", bizNode.path("SupplyEvent").deepCopy());
				if (bizNode.path("DispenseReversal").isObject())   root.set("DispenseReversal", bizNode.path("DispenseReversal").deepCopy());
				if (bizNode.path("OtherMedication").isObject())    root.set("OtherMedication", bizNode.path("OtherMedication").deepCopy());
				if (bizNode.path("Consent").isObject())            root.set("Consent", bizNode.path("Consent").deepCopy());
				if (bizNode.path("AdverseReaction").isObject())    root.set("AdverseReaction", bizNode.path("AdverseReaction").deepCopy());
				if (bizNode.path("Allergy").isObject())            root.set("Allergy", bizNode.path("Allergy").deepCopy());
				if (bizNode.path("MedicalCondition").isObject())   root.set("MedicalCondition", bizNode.path("MedicalCondition").deepCopy());
				if (bizNode.path("ProfessionalService").isObject())root.set("ProfessionalService", bizNode.path("ProfessionalService").deepCopy());
				if (bizNode.path("BasicObservation").isObject())   root.set("BasicObservation", bizNode.path("BasicObservation").deepCopy());
				if (bizNode.path("Polling").isObject())            root.set("Polling", bizNode.path("Polling").deepCopy());
				if (bizNode.path("Subscribe").isObject())          root.set("Subscribe", bizNode.path("Subscribe").deepCopy());
				if (bizNode.path("Password").isObject())           root.set("Password", bizNode.path("Password").deepCopy());
			}
		}

		return mapper.writeValueAsString(root);
	}

	// ---- private helpers ----

	private static ObjectNode buildHeader(ObjectMapper m, String messageId, EventConfig cfg, String creationTime) {
		ObjectNode n = m.createObjectNode();
		n.put("messageId", messageId);
		n.put("creationTime", creationTime);
		n.put("interactionId", cfg.interactionId());
		n.put("versionCode", cfg.versionCode());
		n.put("province", cfg.province());
		// Static CeRx envelope fields that every COMT_IN*CA message requires.
		n.put("interactionIdRoot", "2.16.840.1.113883.1.6");
		n.put("profileId", "CeRx_V01R04.3");
		n.put("processingCode", "T");
		n.put("processingModeCode", "T");
		n.put("acceptAckCode", "AL");
		return n;
	}

	private static ObjectNode buildReceiver(ObjectMapper m) {
		ObjectNode n = m.createObjectNode();
		n.put("deviceId", "HIAL");
		n.put("organizationId", "NLCHI");
		n.put("organizationName", "Newfoundland and Labrador Centre for Health Information");
		return n;
	}

	private static ObjectNode buildDefaultSender(ObjectMapper m) {
		ObjectNode n = m.createObjectNode();
		n.put("ehrUserId", "USER-001");
		n.put("ehrRole", "Pharmacist");
		n.put("facilityId", "FAC-0001");
		n.put("softwareId", "SolisPOS");
		n.put("softwareVersion", "1.0.0");
		return n;
	}

	private static ObjectNode buildDefaultPatient(ObjectMapper m) {
		ObjectNode n = m.createObjectNode();
		n.put("phn", "123456789");
		n.put("firstName", "JANE");
		n.put("lastName", "DOE");
		n.put("dob", "19800101");
		n.put("gender", "F");
		return n;
	}

	private static ObjectNode buildDefaultLocation(ObjectMapper m) {
		ObjectNode n = m.createObjectNode();
		n.put("locationId", "PHARM-001");
		n.put("locationOid", "2.16.840.1.113883.4.277");
		return n;
	}

	private static ObjectNode buildDefaultAnnotation(ObjectMapper m) {
		ObjectNode n = m.createObjectNode();
		n.put("noteText", "Patient prefers generic medications when available.");
		n.put("noteType", "PATPREF");
		return n;
	}

	/** Default Annotation payload for COMT_IN300101CA (deprecate existing patient note). */
	private static ObjectNode buildDefaultDeprecateAnnotation(ObjectMapper m) {
		ObjectNode n = m.createObjectNode();
		n.put("priorNoteId", "11111111-2222-3333-4444-555555555555");
		n.put("reasonCode", "ERROR");
		n.put("reasonText", "Note entered against the wrong patient.");
		return n;
	}

	/** Default Query payload for COMT_IN300201CA (patient note query). */
	private static ObjectNode buildDefaultQuery(ObjectMapper m) {
		ObjectNode n = m.createObjectNode();
		n.put("noteTypeCode", "");
		n.put("fromDate", "20250101");
		n.put("toDate",   "20261231");
		return n;
	}

	/**
	 * Default Query payload for NLPN Section 15 medication / device queries plus
	 * the Section 17-21 clinical observation query siblings.
	 *
	 * <p>The Query node carries common fields (queryId, fromDate, toDate,
	 * simulate) plus an interaction-specific filter (e.g. dispenseId, rxId,
	 * din, prescriberLicense). Builders downstream pick up only the fields
	 * relevant to their parameterList and ignore the rest.</p>
	 *
	 * <p>Supported {@code simulate} values (interpreted by the enrichment-service
	 * HIAL simulator):
	 * <ul>
	 *   <li>{@code ""} (blank) - default: response message carrying one canned
	 *       sample record so the UI round-trip is visible.</li>
	 *   <li>{@code "empty"} - response message with {@code statusCode="NF"} and
	 *       zero subjects (empty result set).</li>
	 *   <li>{@code "refused"} - generic {@code MCCI_IN000003CA} application
	 *       reject ack.</li>
	 * </ul>
	 */
	private static ObjectNode buildDefaultNlpnQuery(ObjectMapper m, String interactionId) {
		ObjectNode n = m.createObjectNode();
		n.put("queryId", "QRY-2026-00001");
		switch (interactionId) {
			case "PORX_IN060050CA" -> n.put("dispenseId", "DSP-DEV-2026-00301");
			case "PORX_IN060210CA" -> n.put("dispenseId", "DSP-2026-00201");
			case "PORX_IN060090CA", "PORX_IN060110CA" -> n.put("rxId", "RX-2026-00102");
			case "PORX_IN060170CA", "PORX_IN060250CA", "PORX_IN060270CA" -> n.put("rxId", "RX-2026-00101");
			case "PORX_IN060450CA" -> n.put("din", "00559407");
			case "PORX_IN060470CA", "PORX_IN060490CA" -> n.put("prescriberLicense", "DR-12345");
			default -> { /* date-range only filters - profiles, summaries, observation queries */ }
		}
		n.put("fromDate", "20250101");
		n.put("toDate", "20261231");
		n.put("simulate", "");
		return n;
	}

	/** Default RecordReference for COMT_IN301001CA (add note against a specific record). */
	private static ObjectNode buildDefaultRecordReference(ObjectMapper m) {
		ObjectNode n = m.createObjectNode();
		n.put("targetId", "RX-2026-00042");
		n.put("targetTypeCode", "RXE");
		return n;
	}

	/** Default Annotation payload for COMT_IN301001CA (add note to record). */
	private static ObjectNode buildDefaultRecordAnnotation(ObjectMapper m) {
		ObjectNode n = m.createObjectNode();
		n.put("noteText", "Counselled patient on interaction risks for this prescription.");
		n.put("noteType", "RXCNSL");
		return n;
	}

	/** Default Retract payload for COMT_IN600001CA (retract action). */
	private static ObjectNode buildDefaultRetract(ObjectMapper m) {
		ObjectNode n = m.createObjectNode();
		n.put("targetEventId", "EVT-TO-RETRACT-0001");
		n.put("simulate", "");
		return n;
	}

	/** Default IssueManagement payload for PORX_IN050001CA (Issue Management / DUR Override). */
	private static ObjectNode buildDefaultIssueManagement(ObjectMapper m) {
		ObjectNode n = m.createObjectNode();
		n.put("targetRxId", "RX-2026-00042");
		n.put("issueCode", "DDI");
		n.put("issueText", "Drug-drug interaction with current therapy.");
		n.put("managementCode", "OVERRIDE");
		n.put("managementText", "Prescriber aware; benefit outweighs risk.");
		n.put("simulate", "");
		return n;
	}

	/** Default Refusal payload for PORX_IN090001CA (Refusal to Dispense). */
	private static ObjectNode buildDefaultRefusal(ObjectMapper m) {
		ObjectNode n = m.createObjectNode();
		n.put("targetRxId", "RX-2026-00042");
		n.put("refusalReasonCode", "ALGY");
		n.put("refusalReasonText", "Documented allergy to prescribed medication.");
		n.put("simulate", "");
		return n;
	}

	/** Default Transfer payload for PORX_IN110001CA (Dispense Responsibility Transfer). */
	private static ObjectNode buildDefaultTransfer(ObjectMapper m) {
		ObjectNode n = m.createObjectNode();
		n.put("targetDispenseId", "DSP-2026-00099");
		n.put("newResponsibleProviderId", "USER-777");
		n.put("newResponsibleFacilityId", "FAC-0099");
		n.put("reasonCode", "TRANSFER");
		n.put("reasonText", "Dispense responsibility transferred to covering pharmacist.");
		n.put("simulate", "");
		return n;
	}

	/**
	 * Default Prescription payload for PORX_IN010380CA (Activate prescription request).
	 *
	 * <p>Supported {@code simulate} values (interpreted by the enrichment-service HIAL
	 * simulator - see {@code HialClientService.buildActivateRxSimulatedAck}):
	 * <ul>
	 *   <li>{@code ""} (blank) - accepted: inbound {@code PORX_IN010390CA}.</li>
	 *   <li>{@code "refused"} - refused: inbound {@code PORX_IN010400CA} with
	 *       {@code detectedIssueEvent}.</li>
	 *   <li>{@code "predetermination-ok"} - NLPN Section 8 inbound
	 *       {@code PORX_IN010640CA} (clinical pre-determination ok).</li>
	 *   <li>{@code "predetermination-not-ok"} - NLPN Section 8 inbound
	 *       {@code PORX_IN010630CA} (clinical pre-determination not ok, with DUR
	 *       {@code detectedIssueEvent}).</li>
	 * </ul>
	 */
	private static ObjectNode buildDefaultRxCreate(ObjectMapper m) {
		ObjectNode n = m.createObjectNode();
		n.put("rxId", "RX-2026-00101");
		n.put("din", "02443147");
		n.put("drugName", "LIPITOR 20 MG TABLET");
		n.put("quantity", "30");
		n.put("daysSupply", "30");
		n.put("directions", "Take 1 tablet by mouth once daily at bedtime.");
		n.put("refills", "5");
		n.put("prescriberLicense", "DR-12345");
		n.put("prescriberOid", "2.16.840.1.113883.4.277");
		n.put("effectiveTime", "20260407");
		n.put("simulate", "");
		return n;
	}

	/**
	 * Default Prescription payload for PORX_IN010300CA (Activate device prescription request).
	 *
	 * <p>Supported {@code simulate} values (interpreted by the enrichment-service HIAL
	 * simulator - see {@code HialClientService.buildActivateDeviceRxSimulatedAck}):
	 * <ul>
	 *   <li>{@code ""} (blank) - accepted: inbound {@code PORX_IN011040CA}.</li>
	 *   <li>{@code "refused"} - refused: inbound {@code PORX_IN011050CA} with
	 *       {@code detectedIssueEvent}.</li>
	 *   <li>{@code "predetermination-ok"} - NLPN Section 8 inbound
	 *       {@code PORX_IN010640CA} (clinical pre-determination ok).</li>
	 *   <li>{@code "predetermination-not-ok"} - NLPN Section 8 inbound
	 *       {@code PORX_IN010630CA} (clinical pre-determination not ok, with DUR
	 *       {@code detectedIssueEvent}).</li>
	 * </ul>
	 */
	private static ObjectNode buildDefaultRxDeviceCreate(ObjectMapper m) {
		ObjectNode n = m.createObjectNode();
		n.put("rxId", "RX-2026-00102");
		n.put("deviceCode", "ACC-GLUC-METER");
		n.put("deviceName", "BLOOD GLUCOSE METER");
		n.put("deviceQuantity", "1");
		n.put("directions", "Use as directed for blood glucose self-monitoring.");
		n.put("refills", "0");
		n.put("prescriberLicense", "DR-12345");
		n.put("prescriberOid", "2.16.840.1.113883.4.277");
		n.put("effectiveTime", "20260407");
		n.put("simulate", "");
		return n;
	}

	/**
	 * Default RxLifecycle payload shared across PORX_IN010440CA (Suspend),
	 * PORX_IN010520CA (Resume), PORX_IN010560CA (Abort Dispense Auth) and
	 * PORX_IN010840CA (Abort Prescription). Reason defaults vary slightly
	 * by interaction so the UI shows a clinically reasonable default.
	 */
	private static ObjectNode buildDefaultRxLifecycle(ObjectMapper m, String interactionId) {
		ObjectNode n = m.createObjectNode();
		switch (interactionId) {
			case "PORX_IN010440CA" -> {
				n.put("targetRxId", "RX-2026-00101");
				n.put("reasonCode", "HOSP");
				n.put("reasonText", "Patient hospitalised; suspend outpatient therapy until discharge.");
			}
			case "PORX_IN010520CA" -> {
				n.put("targetRxId", "RX-2026-00101");
				n.put("reasonCode", "DISCHG");
				n.put("reasonText", "Patient discharged from hospital; resume outpatient therapy.");
			}
			case "PORX_IN010560CA" -> {
				n.put("targetRxId", "DSP-AUTH-2026-00055");
				n.put("reasonCode", "PATREQ");
				n.put("reasonText", "Patient requested cancellation of remaining dispense authorization.");
			}
			default -> {
				n.put("targetRxId", "RX-2026-00101");
				n.put("reasonCode", "THERCH");
				n.put("reasonText", "Therapy changed; abort this prescription.");
			}
		}
		n.put("effectiveTime", "20260407");
		n.put("simulate", "");
		return n;
	}

	/**
	 * Default Dispense payload for PORX_IN020190CA (Record medication dispense processing).
	 *
	 * <p>Supported {@code simulate} values (interpreted by the enrichment-service HIAL
	 * simulator - see {@code HialClientService.buildRecordDispenseSimulatedAck}):
	 * <ul>
	 *   <li>{@code ""} (blank) - accepted: inbound {@code PORX_IN020130CA}.</li>
	 *   <li>{@code "refused"} - refused: inbound {@code PORX_IN020140CA} with
	 *       {@code detectedIssueEvent}.</li>
	 * </ul>
	 */
	private static ObjectNode buildDefaultDispense(ObjectMapper m) {
		ObjectNode n = m.createObjectNode();
		n.put("dispenseId", "DSP-2026-00201");
		n.put("prescriptionId", "RX-2026-00101");
		n.put("din", "02443147");
		n.put("drugName", "LIPITOR 20 MG TABLET");
		n.put("quantity", "30");
		n.put("daysSupply", "30");
		n.put("lotNumber", "LOT-0407-A");
		n.put("expirationDate", "20270407");
		n.put("dispensedBy", "USER-001");
		n.put("effectiveTime", "20260407");
		n.put("simulate", "");
		return n;
	}

	/**
	 * Default DeviceDispense payload for PORX_IN020060CA (Record device dispense processing).
	 *
	 * <p>Supported {@code simulate} values (interpreted by the enrichment-service HIAL
	 * simulator - see {@code HialClientService.buildRecordDeviceDispenseSimulatedAck}):
	 * <ul>
	 *   <li>{@code ""} (blank) - accepted: inbound {@code PORX_IN020340CA}.</li>
	 *   <li>{@code "refused"} - refused: inbound {@code PORX_IN020330CA} with
	 *       {@code detectedIssueEvent}.</li>
	 * </ul>
	 */
	private static ObjectNode buildDefaultDeviceDispense(ObjectMapper m) {
		ObjectNode n = m.createObjectNode();
		n.put("dispenseId", "DSP-DEV-2026-00301");
		n.put("prescriptionId", "RX-2026-00102");
		n.put("deviceCode", "ACC-GLUC-METER");
		n.put("deviceName", "BLOOD GLUCOSE METER");
		n.put("deviceQuantity", "1");
		n.put("serialNumber", "SN-ABC-123456");
		n.put("lotNumber", "LOT-DEV-0407");
		n.put("expirationDate", "20270407");
		n.put("dispensedBy", "USER-001");
		n.put("effectiveTime", "20260407");
		n.put("simulate", "");
		return n;
	}

	/**
	 * Default Pickup payload for PORX_IN020080CA (Record dispense pickup request).
	 *
	 * <p>Supported {@code simulate} values (interpreted by the enrichment-service HIAL
	 * simulator - see {@code HialClientService.buildRecordPickupSimulatedAck}):
	 * <ul>
	 *   <li>{@code ""} (blank) - accepted: inbound {@code PORX_IN020090CA}.</li>
	 *   <li>{@code "refused"} - refused: inbound {@code PORX_IN020100CA} with
	 *       {@code detectedIssueEvent}.</li>
	 * </ul>
	 */
	private static ObjectNode buildDefaultPickup(ObjectMapper m) {
		ObjectNode n = m.createObjectNode();
		n.put("dispenseId", "DSP-2026-00201");
		n.put("pickedUpBy", "PATIENT");
		n.put("effectiveTime", "20260407");
		n.put("simulate", "");
		return n;
	}

	/**
	 * Default SupplyEvent payload for PORX_IN020210CA (Record supply event / ward stock).
	 *
	 * <p>Supported {@code simulate} values (interpreted by the enrichment-service HIAL
	 * simulator - see {@code HialClientService.buildRecordSupplyEventSimulatedAck}):
	 * <ul>
	 *   <li>{@code ""} (blank) - accepted: inbound {@code PORX_IN020220CA}.</li>
	 *   <li>{@code "refused"} - refused: inbound {@code PORX_IN020230CA} with
	 *       {@code detectedIssueEvent}.</li>
	 * </ul>
	 */
	private static ObjectNode buildDefaultSupplyEvent(ObjectMapper m) {
		ObjectNode n = m.createObjectNode();
		n.put("din", "02345678");
		n.put("drugName", "ACETAMINOPHEN 500 MG TABLET");
		n.put("quantity", "500");
		n.put("facilityId", "FAC-WARD-001");
		n.put("lotNumber", "LOT-WS-0407");
		n.put("expirationDate", "20270407");
		n.put("effectiveTime", "20260407");
		n.put("simulate", "");
		return n;
	}

	/**
	 * Default DispenseReversal payload for PORX_IN020370CA (Record dispense reversal).
	 *
	 * <p>Supported {@code simulate} values (interpreted by the enrichment-service HIAL
	 * simulator - see {@code HialClientService.buildRecordDispenseReversalSimulatedAck}):
	 * <ul>
	 *   <li>{@code ""} (blank) - accepted: inbound {@code PORX_IN020380CA}.</li>
	 *   <li>{@code "refused"} - refused: inbound {@code PORX_IN020390CA} with
	 *       {@code detectedIssueEvent}.</li>
	 * </ul>
	 */
	private static ObjectNode buildDefaultDispenseReversal(ObjectMapper m) {
		ObjectNode n = m.createObjectNode();
		n.put("dispenseId", "DSP-2026-00201");
		n.put("reasonCode", "NOTPICK");
		n.put("reasonText", "Medication not picked up by patient; reversed.");
		n.put("effectiveTime", "20260407");
		n.put("simulate", "");
		return n;
	}

	/**
	 * Default OtherMedication payload shared by PORX_IN040020CA (Record) and
	 * PORX_IN040070CA (Update) - NLPN Section 14 (Other Medication / OTC).
	 *
	 * <p>Supported {@code simulate} values (interpreted by the enrichment-service HIAL
	 * simulator): {@code ""} -> accepted (PORX_IN040030CA / PORX_IN040080CA);
	 * {@code "refused"} -> refused (PORX_IN040040CA / PORX_IN040090CA).</p>
	 */
	private static ObjectNode buildDefaultOtherMedication(ObjectMapper m) {
		ObjectNode n = m.createObjectNode();
		n.put("medicationId", "OTC-2026-00501");
		n.put("din", "00559407");
		n.put("gtin", "00612345678901");
		n.put("description", "IBUPROFEN 200 MG TABLET (OTC)");
		n.put("quantity", "50");
		n.put("dosageInstructions", "Take 1-2 tablets every 4-6 hours as needed for pain.");
		n.put("effectiveTime", "20260407");
		n.put("simulate", "");
		return n;
	}

	/**
	 * Default Consent payload for RCMR_IN010003CA - NLPN Section 16 (Consent).
	 *
	 * <p>Supported {@code simulate} values: {@code ""} -> accepted generic MCCI_IN000002CA;
	 * {@code "refused"} -> refused generic MCCI_IN000003CA (spec does not define
	 * flow-specific inbound ids for consent, so the standard CeRx ack/nack is used).</p>
	 */
	private static ObjectNode buildDefaultConsent(ObjectMapper m) {
		ObjectNode n = m.createObjectNode();
		n.put("consentId", "CNS-2026-00101");
		n.put("patientId", "123456789");
		n.put("scope", "record");
		n.put("reason", "Patient has consented to sharing their pharmacy record with the pharmacy team.");
		n.put("effectiveTime", "20260407");
		n.put("simulate", "");
		return n;
	}

	/**
	 * Default AdverseReaction payload shared by REPC_IN000004CA (Record) and
	 * REPC_IN000008CA (Update) - NLPN Section 17 (Adverse Reactions).
	 *
	 * <p>Supported {@code simulate} values: {@code ""} -> accepted
	 * (REPC_IN000005CA / REPC_IN000009CA); {@code "refused"} -> refused
	 * (REPC_IN000006CA / REPC_IN000010CA).</p>
	 */
	private static ObjectNode buildDefaultAdverseReaction(ObjectMapper m) {
		ObjectNode n = m.createObjectNode();
		n.put("reactionId", "AR-2026-00101");
		n.put("agentCode", "00559407");
		n.put("agentDescription", "IBUPROFEN");
		n.put("severity", "moderate");
		n.put("manifestation", "Generalised urticaria; pruritus on trunk.");
		n.put("onsetDate", "20260405");
		n.put("simulate", "");
		return n;
	}

	/**
	 * Default Allergy payload shared by REPC_IN000012CA (Add) and
	 * REPC_IN000020CA (Update) - NLPN Section 18 (Allergy / Intolerance).
	 *
	 * <p>Supported {@code simulate} values: {@code ""} -> accepted
	 * (REPC_IN000013CA / REPC_IN000021CA); {@code "refused"} -> refused
	 * (REPC_IN000014CA / REPC_IN000022CA).</p>
	 */
	private static ObjectNode buildDefaultAllergy(ObjectMapper m) {
		ObjectNode n = m.createObjectNode();
		n.put("allergyId", "ALG-2026-00101");
		n.put("agentCode", "PENICILLIN");
		n.put("agentDescription", "Penicillin-class antibiotics");
		n.put("type", "allergy");
		n.put("severity", "severe");
		n.put("manifestation", "Anaphylaxis; documented history of airway involvement.");
		n.put("onsetDate", "20200315");
		n.put("simulate", "");
		return n;
	}

	/**
	 * Default MedicalCondition payload shared by REPC_IN000028CA (Record) and
	 * REPC_IN000032CA (Update) - NLPN Section 19 (Medical Conditions).
	 *
	 * <p>Supported {@code simulate} values: {@code ""} -> accepted
	 * (REPC_IN000029CA / REPC_IN000033CA); {@code "refused"} -> refused
	 * (REPC_IN000030CA / REPC_IN000034CA).</p>
	 */
	private static ObjectNode buildDefaultMedicalCondition(ObjectMapper m) {
		ObjectNode n = m.createObjectNode();
		n.put("conditionId", "COND-2026-00101");
		n.put("code", "I10");
		n.put("codeSystem", "ICD-10-CA");
		n.put("description", "Essential (primary) hypertension.");
		n.put("onsetDate", "20240201");
		n.put("status", "active");
		n.put("simulate", "");
		return n;
	}

	/**
	 * Default ProfessionalService payload for REPC_IN000044CA - NLPN Section 20
	 * (Professional Services).
	 *
	 * <p>Supported {@code simulate} values: {@code ""} -> accepted (REPC_IN000045CA);
	 * {@code "refused"} -> refused (REPC_IN000046CA).</p>
	 */
	private static ObjectNode buildDefaultProfessionalService(ObjectMapper m) {
		ObjectNode n = m.createObjectNode();
		n.put("serviceId", "SVC-2026-00101");
		n.put("code", "MEDREVW");
		n.put("description", "Comprehensive medication review performed with the patient.");
		n.put("effectiveTime", "20260407");
		n.put("performerId", "USER-001");
		n.put("simulate", "");
		return n;
	}

	/**
	 * Default BasicObservation payload for REPC_IN000051CA - NLPN Section 21
	 * (Basic Observations).
	 *
	 * <p>Supported {@code simulate} values: {@code ""} -> accepted (REPC_IN000052CA);
	 * {@code "refused"} -> refused (REPC_IN000053CA).</p>
	 */
	private static ObjectNode buildDefaultBasicObservation(ObjectMapper m) {
		ObjectNode n = m.createObjectNode();
		n.put("observationId", "OBS-2026-00101");
		n.put("code", "8480-6");
		n.put("codeSystem", "LOINC");
		n.put("description", "Systolic blood pressure");
		n.put("value", "128");
		n.put("unit", "mm[Hg]");
		n.put("effectiveTime", "202604071430");
		n.put("simulate", "");
		return n;
	}

	/**
	 * Default Query payload for PRPA_IN101103CA - NLPN Section 22 Client Registry
	 * Find Candidates Query. Demographic-only search (no PHN required).
	 *
	 * <p>Supported {@code simulate} values: {@code ""} -> default response with one
	 * candidate (PRPA_IN101104CA); {@code "empty"} -> zero-candidate response;
	 * {@code "refused"} -> generic MCCI_IN000003CA application reject.</p>
	 */
	private static ObjectNode buildDefaultCrFindCandidatesQuery(ObjectMapper m) {
		ObjectNode n = m.createObjectNode();
		n.put("queryId", "CR-FIND-2026-00001");
		n.put("firstName", "JANE");
		n.put("lastName", "DOE");
		n.put("dob", "19800101");
		n.put("gender", "F");
		n.put("simulate", "");
		return n;
	}

	/**
	 * Default Query payload for PRPA_IN101101CA - NLPN Section 22 Client Registry
	 * Get Person Demographics Query. Lookup by PHN.
	 *
	 * <p>Supported {@code simulate} values: {@code ""} -> default response with
	 * demographics (PRPA_IN101102CA); {@code "empty"} -> not-found response;
	 * {@code "refused"} -> generic MCCI_IN000003CA application reject.</p>
	 */
	private static ObjectNode buildDefaultCrGetDemographicsQuery(ObjectMapper m) {
		ObjectNode n = m.createObjectNode();
		n.put("queryId", "CR-GETDEM-2026-00001");
		n.put("simulate", "");
		return n;
	}

	/**
	 * Default Polling node shared by MCCI_IN100001CA (Send Poll Request) and
	 * MCCI_IN100004CA (Poll &amp; Fetch Next).
	 *
	 * <p>Supported {@code simulate} values: {@code ""} -> empty queue
	 * (MCCI_IN100003CA); {@code "next"} -> queued message returned;
	 * {@code "refused"} -> exception ack (MCCI_IN100005CA).</p>
	 */
	private static ObjectNode buildDefaultPollingRequest(ObjectMapper m) {
		ObjectNode n = m.createObjectNode();
		n.put("queueId", "FAC-0001-INBOX");
		n.put("simulate", "");
		return n;
	}

	/** Default Polling exception payload for MCCI_IN100005CA (Send Poll Exception Ack). */
	private static ObjectNode buildDefaultPollingException(ObjectMapper m) {
		ObjectNode n = m.createObjectNode();
		n.put("queueId", "FAC-0001-INBOX");
		n.put("exceptionCode", "POLL_PAYLOAD_INVALID");
		n.put("exceptionText", "Polled message payload could not be deserialised by the POS.");
		n.put("simulate", "");
		return n;
	}

	/** Default Query payload for NLPN_IN100120CA (Broadcast Topics query). */
	private static ObjectNode buildDefaultBroadcastTopicsQuery(ObjectMapper m) {
		ObjectNode n = m.createObjectNode();
		n.put("queryId", "BCAST-TOPICS-2026-00001");
		n.put("topicCategory", "");
		n.put("simulate", "");
		return n;
	}

	/** Default Subscribe payload for NLPN_IN100140CA (Subscribe Broadcast Topic request). */
	private static ObjectNode buildDefaultBroadcastSubscribe(ObjectMapper m) {
		ObjectNode n = m.createObjectNode();
		n.put("subscriptionId", "SUB-2026-00001");
		n.put("topicId", "TOPIC-OUTAGE");
		n.put("topicName", "System Outage Notifications");
		n.put("simulate", "");
		return n;
	}

	/** Default Password payload for NLPN_IN100200CA (Update Password request). */
	private static ObjectNode buildDefaultPasswordUpdate(ObjectMapper m) {
		ObjectNode n = m.createObjectNode();
		n.put("currentPasswordHash", "OLDPWHASH-DO-NOT-LOG");
		n.put("newPasswordHash", "NEWPWHASH-DO-NOT-LOG");
		n.put("simulate", "");
		return n;
	}

	/** Copies an incoming object node if present, otherwise returns the provided default. */
	private static ObjectNode copyOrDefault(ObjectMapper m, JsonNode incoming, ObjectNode fallback) {
		if (incoming == null || !incoming.isObject() || incoming.isEmpty()) {
			return fallback;
		}
		return incoming.deepCopy();
	}

	private static String formatCurrentTimestamp() {
		return ZonedDateTime.now(ZoneId.systemDefault()).format(TIMESTAMP_FORMATTER);
	}
}
