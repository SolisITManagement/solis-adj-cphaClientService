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
		Map.entry("nl-add-patient-note",      new EventConfig("COMT_IN300001CA", "V01R04.3", "NL")),
		Map.entry("nl-deprecate-patient-note", new EventConfig("COMT_IN300101CA", "V01R04.3", "NL")),
		Map.entry("nl-patient-note-query",    new EventConfig("COMT_IN300201CA", "V01R04.3", "NL")),
		Map.entry("nl-add-note-to-record",    new EventConfig("COMT_IN301001CA", "V01R04.3", "NL"))
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
			default -> "{}";
		};
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
			default -> {
				// Unknown interaction - copy whatever the caller supplied verbatim for forward-compat.
				if (bizNode.path("Annotation").isObject())       root.set("Annotation", bizNode.path("Annotation").deepCopy());
				if (bizNode.path("Query").isObject())            root.set("Query", bizNode.path("Query").deepCopy());
				if (bizNode.path("RecordReference").isObject())  root.set("RecordReference", bizNode.path("RecordReference").deepCopy());
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
