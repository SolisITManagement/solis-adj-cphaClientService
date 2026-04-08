package com.solis.adj.client.controller;

import java.security.SecureRandom;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.solis.adj.client.service.CphaClientService;

@Controller
public class ClientController {

	private static final Logger log = LoggerFactory.getLogger(ClientController.class);
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

	private final CphaClientService clientService;

	public ClientController(CphaClientService clientService) {
		this.clientService = clientService;
	}

	@GetMapping("/")
	public String index() {
		return "index";
	}

	private String generateRandomId() {
		int length = 20;
		String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
		SecureRandom random = new SecureRandom();
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			sb.append(chars.charAt(random.nextInt(chars.length())));
		}
		return sb.toString();
	}

	private String formatCurrentDateTime() {
		return ZonedDateTime.now(ZoneId.systemDefault()).format(DATE_FORMATTER);
	}

	@GetMapping("/jsononly")
	public String jsonOnly(Model model) {
		String datetime = formatCurrentDateTime();
		String uid = generateRandomId();
		String defaultJson = """
				{
				  "Header": {
				    "UID": "%s",
				    "Store Number": "203",
				    "Type": "request",
				    "Format": "cpha",
				    "Source": "client",
				    "Date": "%s"
				  },
				  "Request": {
				    "IIN": "311901",
				    "Version Number": "03",
				    "Transaction Code": "01",
				    "Provider Software ID": "SO",
				    "Provider Software Version": "V1",
				    "Active Device ID": "",
				    "Pharmacy ID Code": "4646008278",
				    "Provider Transaction Date": "250806",
				    "Trace Number": "003271",
				    "Carrier ID": "10",
				    "Group Number or Code": "0000004949",
				    "Client ID # or Code": "000000049490101",
				    "Patient Code": "",
				    "Patient DOB": "19700101",
				    "Cardholder Identity": "",
				    "Relationship": "0",
				    "Patient First Name": "GRAHAM",
				    "Patient Last Name": "GAIL",
				    "Provincial Health Care ID Code": "",
				    "Patient Gender": "M",
				    "Medical Reason Reference": "",
				    "Medical Condition / Reason for Use": "",
				    "New/Refill Code": "N",
				    "Original Prescription Number": "000434250",
				    "Refill / Repeat Authorizations": "03",
				    "Current Rx Number": "000434251",
				    "DIN /GP# / PIN": "00636622",
				    "SSC": "",
				    "Quantity": "000300",
				    "Days Supply": "030",
				    "Prescriber ID Reference": "51",
				    "Prescriber ID": "190168",
				    "Product Selection": "1",
				    "Unlisted Compound": "",
				    "Special Authorization Number or Code": "",
				    "Intervention and Exception Codes": "",
				    "Drug Cost / Product Value": "000400",
				    "Cost Upcharge": "00000",
				    "Professional Fee": "00500",
				    "Compounding Charge": "00000",
				    "Compounding Time": "00",
				    "Special Services Fee(s)": "00000",
				    "Previously Paid": "000000",
				    "Pharmacist ID": "403872",
				    "Adjudication Date": "000000"
				  }
				}
				""".formatted(uid, datetime);
		model.addAttribute("jsonData", defaultJson);
		return "jsononly";
	}

	@PostMapping("/adjudicate")
	public String submitJson(@RequestParam("jsonData") String jsonData, @RequestParam("action") String action,
			Model model) throws Exception {
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		log.info("Incoming adjudicate action: {}\nrequest: {}", action, jsonData);
		JsonAccentRemover remover = new JsonAccentRemover();
		jsonData = remover.removeAccentsFromNameFields(jsonData);
		log.debug("After accent removal - action: {}\nrequest: {}", action, jsonData);
		String response = "";
		if ("adjudicate".equalsIgnoreCase(action)) {
			response = clientService.sendJsonToPublisher(jsonData);
		} else {
			ObjectMapper reqmapper = new ObjectMapper();
			ObjectNode root = (ObjectNode) reqmapper.readTree(jsonData);
			JsonNode header = root.path("Header");
			String uid = header.path("UID").asText();
			response = clientService.retry(uid);
		}
		log.info("Outgoing adjudicate response: {}", response);
		stopWatch.stop();
		double seconds = stopWatch.getTotalTimeMillis() / 1000.0;
		log.info("Adjudication processing took {} seconds", seconds);
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode root;
		try {
			root = (ObjectNode) mapper.readTree(response);

			if (root.has("count")) {
				ObjectNode results = (ObjectNode) root.get("results");
				log.info("Processing bulk claim response");
				for (Iterator<String> it = results.fieldNames(); it.hasNext();) {
					String key = it.next();
					ObjectNode entry = (ObjectNode) results.get(key);
					ObjectNode telusResponse = (ObjectNode) entry.get("CPhA").get("Response");
					if (telusResponse.has("fixed Raw Request")) {
						String raw = telusResponse.get("fixed Raw Request").asText();
						log.debug("Raw request for key {}: {}", key, raw);
						RequestFixedStringToJson requestFixedStringToJson = new RequestFixedStringToJson();
						ObjectNode requestNode = requestFixedStringToJson.decodeFixedRequest(raw, mapper);
						log.debug("Decoded request: {}", requestNode);
						telusResponse.set("Request", requestNode);
					}
				}
			}
		} catch (Exception e) {
			log.warn("Failed to parse adjudication response as JSON, returning raw response", e);
			model.addAttribute("response", response);
			model.addAttribute("jsonData", jsonData);
			model.addAttribute("executionTime", seconds);
			return "jsononly";
		}
		model.addAttribute("response", mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root));
		model.addAttribute("jsonData", jsonData);
		model.addAttribute("executionTime", seconds);
		return "jsononly";
	}

	@GetMapping("/total-and-details")
	public String totalAndDetails(Model model) {
		String datetime = formatCurrentDateTime();
		String uid = generateRandomId();
		String defaultJson = """
				{
				  "Header" :
				  {
				    "UID" : "%s",
				    "Type" : "totals",
				    "Format" : "cpha",
				    "Source" : "client",
				    "Store Number": "1465",
				    "Date" : "%s"
				  },
				  "Request" :
				  {
				    "IIN" : "610068",
				    "Version Number" : "03",
				    "Transaction Code" : "30",
				    "Provider Software ID" : "SD",
				    "Provider Software Version" : "01",
				    "Active Device ID" : "",
				    "Pharmacy ID Code" : "0000101465",
				    "Provider Transaction Date" : "240221",
				    "Trace Number" : "009626",
				    "Carrier ID" : "",
				    "Group Number or Code" : "",
				    "Adjudication Date" : "240220",
				    "Beginning of Record" : "000000000",
				    "End of Record" : "000000000"
				  }
				}
				""".formatted(uid, datetime);
		model.addAttribute("jsonTotData", defaultJson);
		return "tot-req-and-res-json";
	}

	@PostMapping("/totals")
	public String submitTotalsJson(@RequestParam("jsonTotData") String jsonTotData, Model model) {
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		log.info("Incoming totals request: {}", jsonTotData);
		String response = clientService.sendJsonPublishTotals(jsonTotData);
		stopWatch.stop();
		double seconds = stopWatch.getTotalTimeMillis() / 1000.0;
		log.info("Totals processing took {} seconds", seconds);
		model.addAttribute("totResponse", response);
		model.addAttribute("jsonTotData", jsonTotData);
		model.addAttribute("executionTime", seconds);
		return "tot-req-and-res-json";
	}

	// --- View name to event config mapping ---

	private record EventConfig(String eventTypeCode, String interactionId, String hl7Version) {}

	private static final Map<String, EventConfig> EVENT_CONFIG = Map.ofEntries(
		Map.entry("bc-find-candidate",    new EventConfig("HCIM_IN_FindCandidates", "HCIM_IN_FindCandidates", "V3PR1")),
		Map.entry("bc-get-demographics",  new EventConfig("HCIM_IN_GetCandidate",   "HCIM_IN_GetDemographics", "V3PR1")),
		Map.entry("bc-patient-profile",   new EventConfig("TRP",              "TRP",              "V2PR1")),
		Map.entry("bc-prescriber-id",     new EventConfig("TIP",              "TIP",              "V2PR1")),
		Map.entry("bc-claim-reversal",    new EventConfig("TAC_TDU_REVERSAL", "TAC_TDU_REVERSAL", "V2PR1")),
		Map.entry("bc-retrieve-rx",       new EventConfig("TRX_X0",           "TRX_X0",           "V2PR1")),
		Map.entry("bc-record-rx",         new EventConfig("TRX_X1",           "TRX_X1",           "V2PR1")),
		Map.entry("bc-profile-info-update", new EventConfig("TPI",            "TPI",              "V2PR1")),
		Map.entry("bc-update-rx-status",  new EventConfig("TRX_X2",           "TRX_X2",           "V2PR1")),
		Map.entry("bc-adjust-rx",         new EventConfig("TRX_X3",           "TRX_X3",           "V2PR1")),
		Map.entry("bc-location-details",  new EventConfig("TIL",              "TIL",              "V2PR1")),
		Map.entry("bc-dispense-event",    new EventConfig("DISPENSE",         "DISPENSE",         "V2PR1"))
	);

	// --- Simplified GET endpoints (business data only in UI) ---

	@GetMapping("/bc-find-candidate")
	public String findCandidate(Model model) { return serveBcPage(model, "bc-find-candidate"); }

	@GetMapping("/bc-get-demographics")
	public String getDemographics(Model model) { return serveBcPage(model, "bc-get-demographics"); }

	@GetMapping("/bc-patient-profile")
	public String patientProfile(Model model) { return serveBcPage(model, "bc-patient-profile"); }

	@GetMapping("/bc-prescriber-id")
	public String prescriberId(Model model) { return serveBcPage(model, "bc-prescriber-id"); }

	@GetMapping("/bc-claim-reversal")
	public String claimReversal(Model model) { return serveBcPage(model, "bc-claim-reversal"); }

	@GetMapping("/bc-retrieve-rx")
	public String retrievePrescription(Model model) { return serveBcPage(model, "bc-retrieve-rx"); }

	@GetMapping("/bc-record-rx")
	public String recordPrescription(Model model) { return serveBcPage(model, "bc-record-rx"); }

	@GetMapping("/bc-profile-info-update")
	public String profileInfoUpdate(Model model) { return serveBcPage(model, "bc-profile-info-update"); }

	@GetMapping("/bc-update-rx-status")
	public String updateRxStatus(Model model) { return serveBcPage(model, "bc-update-rx-status"); }

	@GetMapping("/bc-adjust-rx")
	public String adjustRx(Model model) { return serveBcPage(model, "bc-adjust-rx"); }

	@GetMapping("/bc-location-details")
	public String locationDetails(Model model) { return serveBcPage(model, "bc-location-details"); }

	@GetMapping("/bc-dispense-event")
	public String dispenseEvent(Model model) { return serveBcPage(model, "bc-dispense-event"); }

	private String serveBcPage(Model model, String viewName) {
		model.addAttribute("jsonData", getBusinessDataDefault());
		return viewName;
	}

	// --- POST: merge business data with infrastructure, then call Data Service ---

	@PostMapping("/callHl7Event")
	public String publishHl7Event(@RequestParam("jsonData") String jsonData,
			@RequestParam(value = "returnView") String returnView, Model model) {
		String safeView = sanitizeViewName(returnView);
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		String fullPayload;
		String messageId = null;
		try {
			fullPayload = buildFullPayload(jsonData, safeView);
			ObjectMapper payloadMapper = new ObjectMapper();
			messageId = payloadMapper.readTree(fullPayload).path("Header").path("messageId").asText(null);
		} catch (Exception e) {
			log.error("Failed to build full payload from business data", e);
			model.addAttribute("response", "{\"error\": \"Failed to build request: " + e.getMessage() + "\"}");
			model.addAttribute("jsonData", jsonData);
			return safeView;
		}

		log.info("Sending full HL7 event request to Data Service (messageId={})", messageId);
		log.debug("Full payload: {}", fullPayload);
		String response = clientService.sendHL7Request(fullPayload);
		stopWatch.stop();
		double seconds = stopWatch.getTotalTimeMillis() / 1000.0;
		log.info("HL7 event processing took {} seconds", seconds);

		String hl7Request = null;
		String hl7Response = null;
		String formattedResponse = response;
		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode respNode = mapper.readTree(response);
			formattedResponse = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(respNode);
			if (respNode.has("hl7Request")) {
				hl7Request = respNode.path("hl7Request").asText(null);
			}
			if (respNode.has("hl7Response")) {
				hl7Response = respNode.path("hl7Response").asText(null);
			}
		} catch (Exception e) {
			log.debug("Could not parse HL7 fields from response", e);
		}

		model.addAttribute("response", formattedResponse);
		model.addAttribute("hl7Request", hl7Request);
		model.addAttribute("hl7Response", hl7Response);
		model.addAttribute("jsonData", jsonData);
		model.addAttribute("messageId", messageId);
		model.addAttribute("executionTime", seconds);
		return safeView;
	}

	private String sanitizeViewName(String candidate) {
		return EVENT_CONFIG.containsKey(candidate) ? candidate : "bc-find-candidate";
	}

	// --- Business data shown in the UI textarea ---

	private String getBusinessDataDefault() {
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

	// --- Merge business data with infrastructure defaults to produce the full payload ---

	private String buildFullPayload(String businessJson, String viewName) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode bizNode = mapper.readTree(businessJson);
		JsonNode patient = bizNode.path("Patient");
		JsonNode rx = bizNode.path("Prescription");

		EventConfig config = EVENT_CONFIG.get(viewName);
		String datetime = formatCurrentDateTime();
		String messageId = generateRandomId();

		ObjectNode root = mapper.createObjectNode();

		// Header
		ObjectNode header = mapper.createObjectNode();
		header.put("messageIdRoot", "2.16.840.1.113883.3.51.1.1.1");
		header.put("messageId", messageId);
		header.put("creationTime", "20250604092800");
		header.put("versionCode", config.hl7Version());
		header.put("processingCode", "T");
		header.put("processingModeCode", "T");
		header.put("acceptAckCode", "AL");
		header.put("interactionId", config.interactionId());
		header.put("interactionIdRoot", "2.16.840.1.113883.3.51.1.1.2");
		root.set("Header", header);

		// Sender
		ObjectNode sender = mapper.createObjectNode();
		sender.put("deviceExtension", "SO_RX");
		sender.put("organizationExtension", "SO");
		sender.put("senderIdRoot", "2.16.840.1.113883.3.51.1.1.5");
		sender.put("organizationIdRoot", "2.16.840.1.113883.3.51.1.1.3");
		root.set("Sender", sender);

		// Receiver
		ObjectNode receiver = mapper.createObjectNode();
		receiver.put("deviceExtension", "BCHCIM");
		receiver.put("organizationExtension", "HCIM");
		receiver.put("receiverIdRoot", "2.16.840.1.113883.3.51.1.1.4");
		receiver.put("organizationIdRoot", "2.16.840.1.113883.3.51.1.1.3");
		root.set("Receiver", receiver);

		// MSH
		ObjectNode msh = mapper.createObjectNode();
		msh.put("fieldSeparator", "|");
		msh.put("encodingCharacters", "^~\\E\\&");
		msh.put("sendingApplication", "SO");
		msh.put("sendingFacility", "BC00000F97");
		msh.put("receivingApplication", "PNP");
		msh.put("receivingFacility", "ERXPP");
		msh.put("security", "6LFJ9.*2FIS,RQCB.@ZU:34.152.37.242");
		msh.put("messageType", "ZPN");
		msh.put("messageControlId", "00000F97250618365111");
		msh.put("processingId", "P");
		msh.put("versionId", "2.1");
		msh.put("dateTimeOfMessage", datetime);
		root.set("MSH", msh);

		// Candidate (mapped from Patient business data)
		String phn = patient.path("phn").asText("9876543210");
		ObjectNode candidate = mapper.createObjectNode();
		candidate.put("personIdExtension", "PSUSH@BC000001CT");
		candidate.put("birthDate", "19970303");
		candidate.put("lastName", patient.path("lastName").asText("MASK"));
		candidate.put("firstName", patient.path("firstName").asText("GENDER"));
		candidate.put("phn", phn);
		candidate.put("dob", patient.path("dob").asText("19800101"));
		candidate.put("gender", patient.path("gender").asText("M"));
		ObjectNode address = mapper.createObjectNode();
		JsonNode patAddr = patient.path("address");
		address.put("line", patAddr.path("line").asText("123 Main St"));
		address.put("city", patAddr.path("city").asText("Vancouver"));
		address.put("province", patAddr.path("province").asText("BC"));
		address.put("postalCode", patAddr.path("postalCode").asText("V5K0A1"));
		candidate.set("address", address);
		candidate.put("phone", patient.path("phone").asText("6045551234"));
		candidate.put("personIdRoot", "2.16.840.1.113883.3.51.1.1.6.1");
		candidate.put("personIdValue", "9872205077");
		candidate.put("dataEntererRoot", "2.16.840.1.113883.3.51.1.1.7");
		root.set("Candidate", candidate);

		// Event
		ObjectNode event = mapper.createObjectNode();
		event.put("eventTypeCode", config.eventTypeCode());
		event.put("recordedDateTime", datetime);
		root.set("Event", event);

		// Visit
		ObjectNode visit = mapper.createObjectNode();
		visit.put("patientClass", "O");
		visit.put("location", "ER");
		visit.put("admitDateTime", datetime);
		visit.put("providerId", "123456");
		visit.put("providerName", "Smith^Jane");
		root.set("Visit", visit);

		// ZCA
		ObjectNode zca = mapper.createObjectNode();
		zca.put("reserved", "");
		zca.put("binNumber", "70");
		zca.put("carrierId", "X1");
		zca.put("versionNumber", "SD");
		zca.put("transactionCode", "02");
		zca.put("cardholderIdCode", "");
		zca.put("groupNumber", "");
		zca.put("clientCode", "");
		zca.put("softwareId", "");
		zca.put("softwareVersion", "");
		zca.put("recordType", "1");
		zca.put("carrierIdFromHL7", "01");
		root.set("ZCA", zca);

		// ZCB
		ObjectNode zcb = mapper.createObjectNode();
		zcb.put("locationId", "BC00000F97");
		zcb.put("transactionDate", "250618");
		zcb.put("transactionTime", "");
		zcb.put("languageCode", "");
		zcb.put("traceNumber", "365111");
		zcb.put("reserved6", "");
		zcb.put("reserved7", "");
		zcb.put("reserved8", "");
		zcb.put("reserved9", "");
		zcb.put("reserved10", "");
		zcb.put("reserved11", "");
		zcb.put("reserved12", "");
		zcb.put("traceNumberRepeated", "365111");
		zcb.put("locationIdHL7", "BC00000F97");
		zcb.put("transactionDateHL7", "250618");
		zcb.put("traceNumberHL7", "365111");
		root.set("ZCB", zcb);

		// ZCC (PHN from Patient)
		String paddedPhn = "000" + phn;
		ObjectNode zcc = mapper.createObjectNode();
		zcc.put("reserved1", "");
		zcc.put("reserved2", "");
		zcc.put("reserved3", "");
		zcc.put("reserved4", "");
		zcc.put("reserved5", "");
		zcc.put("reserved6", "");
		zcc.put("reserved7", "");
		zcc.put("reserved8", "");
		zcc.put("reserved9", "");
		zcc.put("phn", paddedPhn);
		zcc.put("phnHL7", paddedPhn);
		root.set("ZCC", zcc);

		// ZPX (mapped from Prescription business data)
		String dinGpPin = rx.path("dinGpPin").asText("02489007");
		String quantity = rx.path("quantity").asText("700");
		String daysSupply = rx.path("daysSupply").asText("14");
		String productCost = rx.path("productCost").asText("91");
		String professionalFee = rx.path("professionalFee").asText("99");
		String newRefillCode = rx.path("newRefillCode").asText("N");
		String prescriberId = rx.path("prescriberId").asText("13133135");
		String sigInstructions = rx.path("sigInstructions").asText("");
		String prescriptionNumber = rx.path("prescriptionNumber").asText("13147");

		ObjectNode zpx = mapper.createObjectNode();
		zpx.put("internalRxId", "ZPX1");
		zpx.put("newRefillCode", newRefillCode);
		zpx.put("productSelectionCode", "1");
		zpx.put("dinGpPin", dinGpPin);
		zpx.put("reserved5", "");
		zpx.put("reserved6", "");
		zpx.put("reserved7", "");
		zpx.put("reserved8", "");
		zpx.put("reserved9", "");
		zpx.put("reserved10", "");
		zpx.put("reserved11", "");
		zpx.put("reserved12", "");
		zpx.put("sigCode", "PHARMACY ASSISTANT");
		zpx.put("quantity", quantity);
		zpx.put("daysSupply", daysSupply);
		zpx.put("reserved16", "");
		zpx.put("productCost", productCost);
		zpx.put("reserved18", "");
		zpx.put("professionalFee", professionalFee);
		zpx.put("frequencyIntervention", "OTHERFREQUENCY");
		zpx.put("reserved21", "");
		zpx.put("reserved22", "");
		zpx.put("reserved23", "");
		zpx.put("reserved24", "");
		zpx.put("reserved25", "");
		zpx.put("reserved26", "");
		zpx.put("reserved27", "");
		zpx.put("reserved28", "");
		zpx.put("sigInstructions", sigInstructions);
		zpx.put("reserved30", "");
		zpx.put("reserved31", "");
		zpx.put("reserved32", "");
		zpx.put("reserved33", "");
		zpx.put("reserved34", "");
		zpx.put("reserved35", "");
		zpx.put("prescriberId", prescriberId);
		root.set("ZPX", zpx);

		// ZCD (mapped from Prescription business data)
		ObjectNode zcd = mapper.createObjectNode();
		zcd.put("newRefillCode", newRefillCode);
		zcd.put("din", "21299844");
		zcd.put("gpPin", dinGpPin);
		zcd.put("quantity", quantity);
		zcd.put("daysSupply", daysSupply);
		zcd.put("cost", productCost);
		zcd.put("productId", "33476LO");
		zcd.put("repeatCount", "3");
		zcd.put("pharmacyId", "9615");
		zcd.put("patientId", "1335");
		zcd.put("providerId", "1160");
		zcd.put("prescriptionNumber", prescriptionNumber);
		zcd.put("prescriberId", prescriberId);
		root.set("ZCD", zcd);

		// ZPJ
		ArrayNode zpj = mapper.createArrayNode();
		zpj.add(mapper.createObjectNode().put("id", "ZPJ1"));
		zpj.add(mapper.createObjectNode().put("id", "ZPJ2"));
		zpj.add(mapper.createObjectNode().put("id", "ZPJ2"));
		zpj.add(mapper.createObjectNode().put("id", "ZPJ2"));
		zpj.add(mapper.createObjectNode().put("id", "ZPJ3"));
		ObjectNode zpj4 = mapper.createObjectNode();
		zpj4.put("id", "ZPJ4");
		zpj4.put("instructions", sigInstructions);
		zpj.add(zpj4);
		root.set("ZPJ", zpj);

		// ZZZ
		ObjectNode zzz = mapper.createObjectNode();
		zzz.put("transactionType", "TDU");
		zzz.put("messageStatusCode", "");
		zzz.put("reserved3", "");
		zzz.put("traceNumber", "365111");
		zzz.put("pharmacyId", "P1");
		zzz.put("prescriptionNumber", prescriptionNumber);
		zzz.put("additionalReference", "");
		zzz.put("rejectionCodes", "");
		zzz.put("reserved9", "");
		zzz.put("reserved10", "");
		root.set("ZZZ", zzz);

		// ZZZSegments
		ArrayNode zzzSegments = mapper.createArrayNode();
		ObjectNode seg1 = mapper.createObjectNode();
		seg1.put("transactionType", "TDU");
		seg1.put("traceNumber", "365111");
		seg1.put("pharmacyId", "P1");
		seg1.put("prescriptionNumber", prescriptionNumber);
		zzzSegments.add(seg1);
		ObjectNode seg2 = mapper.createObjectNode();
		seg2.put("transactionType", "TAC");
		seg2.put("traceNumber", "365111");
		seg2.put("pharmacyId", "P1");
		seg2.put("prescriptionNumber", prescriptionNumber);
		zzzSegments.add(seg2);
		root.set("ZZZSegments", zzzSegments);

		return mapper.writeValueAsString(root);
	}

}
