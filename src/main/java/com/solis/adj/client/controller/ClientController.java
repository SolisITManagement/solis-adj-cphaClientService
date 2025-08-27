package com.solis.adj.client.controller;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.solis.adj.client.service.CphaClientService;

@Controller
public class ClientController {

	@Autowired
	private CphaClientService clientService;

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

	@GetMapping("/jsononly")
	public String jsonOnly(Model model) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
		ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
		String datetime = now.format(formatter);
		// 600526, "IIN": "311901",
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
		System.out.println("incoming jsonData action: " + action + "\request: " + jsonData);
		JsonAccentRemover remover = new JsonAccentRemover();
		jsonData = remover.removeAccentsFromNameFields(jsonData);
		System.out.println("incoming jsonData after accent char removed.. action: " + action + "\request: " + jsonData);
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
		System.out.println("outgoing jsonData response: " + response);
		stopWatch.stop();
		double seconds = stopWatch.getTotalTimeMillis() / 1000.0;
		System.out.println("Adjudication processing took " + seconds + " seconds");
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode root;
		try {
			root = (ObjectNode) mapper.readTree(response);

			if (root.has("count")) { // 10k claim - request object parsing
				ObjectNode results = (ObjectNode) root.get("results");
				System.out.println("10k claim: ");
				for (Iterator<String> it = results.fieldNames(); it.hasNext();) {
					String key = it.next();
					ObjectNode entry = (ObjectNode) results.get(key);
					ObjectNode telusResponse = (ObjectNode) entry.get("CPhA").get("Response");
					if (telusResponse.has("fixed Raw Request")) {
						String raw = telusResponse.get("fixed Raw Request").asText();
						System.out.println("telusResponse (Raw request): " + raw);
						RequestFixedStringToJson requestFixedStringToJson = new RequestFixedStringToJson();
						// ObjectNode requestNode = requestFixedStringToJson.parse(raw);
						ObjectNode requestNode = requestFixedStringToJson.decodeFixedRequest(raw, mapper);
						System.out.println("10k jsonData request: " + requestNode.toString());
						telusResponse.set("Request", requestNode);
					}
				}
			}
		} catch (Exception e) {
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
	public String TotalAndDetails(Model model) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
		ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
		String datetime = now.format(formatter);
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
		System.out.println("incoming jsonTotData: " + jsonTotData);
		String response = clientService.sendJsonPublishTotals(jsonTotData);
		stopWatch.stop();
		double seconds = stopWatch.getTotalTimeMillis() / 1000.0;
		System.out.println("Totals Adj processing took " + seconds + " ms");
		model.addAttribute("totResponse", response);
		model.addAttribute("jsonTotData", jsonTotData);
		model.addAttribute("executionTime", seconds);
		return "tot-req-and-res-json";
	}

	@GetMapping("/bc-find-candidate")
	public String findCandidate(Model model) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
		ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
		String datetime = now.format(formatter);
		String messageId = generateRandomId();
		String intractionId = "HCIM_IN_FindCandidates";
		String hl7Version = "V3PR1";
		String defaultJson = setBCDefault().formatted(messageId, hl7Version, intractionId, datetime, intractionId, datetime,
				datetime);
		model.addAttribute("jsonData", defaultJson);
		return "bc-find-candidate";
	}

	@GetMapping("/bc-get-demographics")
	public String getDemographics(Model model) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
		ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
		String datetime = now.format(formatter);
		String messageId = generateRandomId();
		String intractionId = "HCIM_IN_GetDemographics";
		String hl7Version = "V3PR1";
		String defaultJson = setBCDefault().formatted(messageId, hl7Version, intractionId, datetime, intractionId, datetime,
				datetime);
		model.addAttribute("jsonData", defaultJson);
		return "bc-get-demographics";
	}

	@GetMapping("/bc-dispense-event")
	public String dispenseEvent(Model model) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
		ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
		String datetime = now.format(formatter);
		String messageId = generateRandomId();
		String intractionId = "DISPENSE";
		String hl7Version = "V2PR1";
		String defaultJson = setBCDefault().formatted(messageId, // "messageId": "%s",
													hl7Version,  // "versionCode": "%s",													intractionId, // "interactionId": "%s",
													intractionId, // "eventTypeCode": "%s",
													datetime,     // "dateTimeOfMessage": "%s"
													intractionId, 
													datetime,     // "recordedDateTime": "%s"
													datetime);    // "admitDateTime": "%s",
		model.addAttribute("jsonData", defaultJson);
		return "bc-dispense-event";
	}

	@PostMapping("/callHl7Event")
	public String publishHl7Event(@RequestParam("jsonData") String jsonData, @RequestParam(value = "returnView") String returnView, Model model) {
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		System.out.println("incoming jsonData: " + jsonData);
		String response = clientService.sendHL7Request(jsonData);
		stopWatch.stop();
		double seconds = stopWatch.getTotalTimeMillis() / 1000.0;
		System.out.println("Response " + response);
		System.out.println("Totals  processing took " + seconds + " ms");
		model.addAttribute("response", response);
		model.addAttribute("jsonData", jsonData);
		model.addAttribute("executionTime", seconds);
		return sanitizeViewName(returnView);
	}
	
	private String sanitizeViewName(String candidate) {
	    // Prevent open redirects / path traversal: allow only known templates
	    Set<String> allowed = Set.of(
	        "bc-find-candidate",
	        "bc-get-demographics",
	        "bc-dispense-event"
	    );
	    return allowed.contains(candidate) ? candidate : "bc-find-candidate";
	}
	private String setBCDefault() {
		return """

				{
				  "Header": {
				    "messageIdRoot": "2.16.840.1.113883.3.51.1.1.1",
				    "messageId": "%s",
				    "creationTime": "20250604092800",
				    "versionCode": "%s",
				    "processingCode": "T",
				    "processingModeCode": "T",
				    "acceptAckCode": "AL",
				    "interactionId": "%s",
				    "interactionIdRoot": "2.16.840.1.113883.3.51.1.1.2"
				  },
				  "Sender": {
				    "deviceExtension": "SO_RX",
				    "organizationExtension": "SO",
				    "senderIdRoot": "2.16.840.1.113883.3.51.1.1.5",
				    "organizationIdRoot": "2.16.840.1.113883.3.51.1.1.3"
				  },
				  "Receiver": {
				    "deviceExtension": "BCHCIM",
				    "organizationExtension": "HCIM",
				    "receiverIdRoot": "2.16.840.1.113883.3.51.1.1.4",
				    "organizationIdRoot": "2.16.840.1.113883.3.51.1.1.3"
				  },

				  "MSH": {
				    "fieldSeparator": "|",
				    "encodingCharacters": "^~\\E\\&",
				    "sendingApplication": "SO",
				    "sendingFacility": "BC00000F97",
				    "receivingApplication": "PNP",
				    "receivingFacility": "ERXPP",
				    "security": "6LFJ9.*2FIS,RQCB.@ZU:34.152.37.242",
				    "messageType": "ZPN",
				    "messageControlId": "00000F97250618365111",
				    "processingId": "P",
				    "versionId": "2.1",
				    "dateTimeOfMessage": "%s"
				  },

				  "Candidate": {
				    "personIdExtension": "PSUSH@BC000001CT",
				    "birthDate": "19970303",
				    "lastName": "MASK",
				    "firstName": "GENDER",
				    "phn": "9876543210",
				    "dob": "19800101",
				    "gender": "M",
				    "address": {
				      "line": "123 Main St",
				      "city": "Vancouver",
				      "province": "BC",
				      "postalCode": "V5K0A1"
				    },
				    "phone": "6045551234",
				    "personIdRoot": "2.16.840.1.113883.3.51.1.1.6.1",
				    "personIdValue": "9872205077",
				    "dataEntererRoot": "2.16.840.1.113883.3.51.1.1.7"
				  },
				  "Event": {
				    "eventTypeCode": "%s",
				    "recordedDateTime": "%s"
				  },
				  "Visit": {
				    "patientClass": "O",
				    "location": "ER",
				    "admitDateTime": "%s",
				    "providerId": "123456",
				    "providerName": "Smith^Jane"
				  },

				  "ZCA": {
				    "reserved": "",
				    "binNumber": "70",
				    "carrierId": "X1",
				    "versionNumber": "SD",
				    "transactionCode": "02",
				    "cardholderIdCode": "",
				    "groupNumber": "",
				    "clientCode": "",
				    "softwareId": "",
				    "softwareVersion": "",
				    "recordType": "1",
				    "carrierIdFromHL7": "01"
				  },

				  "ZCB": {
				    "locationId": "BC00000F97",
				    "transactionDate": "250618",
				    "transactionTime": "",
				    "languageCode": "",
				    "traceNumber": "365111",
				    "reserved6": "",
				    "reserved7": "",
				    "reserved8": "",
				    "reserved9": "",
				    "reserved10": "",
				    "reserved11": "",
				    "reserved12": "",
				    "traceNumberRepeated": "365111",
				    "locationIdHL7": "BC00000F97",
				    "transactionDateHL7": "250618",
				    "traceNumberHL7": "365111"
				  },

				  "ZCC": {
				    "reserved1": "",
				    "reserved2": "",
				    "reserved3": "",
				    "reserved4": "",
				    "reserved5": "",
				    "reserved6": "",
				    "reserved7": "",
				    "reserved8": "",
				    "reserved9": "",
				    "phn": "0009645099336",
				    "phnHL7": "0009645099336"
				  },

				  "ZPX": {
				    "internalRxId": "ZPX1",
				    "newRefillCode": "N",
				    "productSelectionCode": "1",
				    "dinGpPin": "02489007",
				    "reserved5": "",
				    "reserved6": "",
				    "reserved7": "",
				    "reserved8": "",
				    "reserved9": "",
				    "reserved10": "",
				    "reserved11": "",
				    "reserved12": "",
				    "sigCode": "PHARMACY ASSISTANT",
				    "quantity": "700",
				    "daysSupply": "14",
				    "reserved16": "",
				    "productCost": "91",
				    "reserved18": "",
				    "professionalFee": "99",
				    "frequencyIntervention": "OTHERFREQUENCY",
				    "reserved21": "",
				    "reserved22": "",
				    "reserved23": "",
				    "reserved24": "",
				    "reserved25": "",
				    "reserved26": "",
				    "reserved27": "",
				    "reserved28": "",
				    "sigInstructions": "APPLY TO FULL FACE ( EXCEPT AROUND EYES ) STARTING TWICE WEEKLY ( INCREASE TO...",
				    "reserved30": "",
				    "reserved31": "",
				    "reserved32": "",
				    "reserved33": "",
				    "reserved34": "",
				    "reserved35": "",
				    "prescriberId": "13133135"
				  },

				  "ZCD": {
				    "newRefillCode": "N",
				    "din": "21299844",
				    "gpPin": "02489007",
				    "quantity": "700",
				    "daysSupply": "14",
				    "cost": "91",
				    "productId": "33476LO",
				    "repeatCount": "3",
				    "pharmacyId": "9615",
				    "patientId": "1335",
				    "providerId": "1160",
				    "prescriptionNumber": "13147",
				    "prescriberId": "13133135"
				  },

				  "ZPJ": [
				    { "id": "ZPJ1" },
				    { "id": "ZPJ2" },
				    { "id": "ZPJ2" },
				    { "id": "ZPJ2" },
				    { "id": "ZPJ3" },
				    { "id": "ZPJ4", "instructions": "APPLY TO FULL FACE ( EXCEPT AROUND EYES ) STARTING TWICE WEEKLY ( INCREASE TO..." }
				  ],

				  "ZZZ": {
				    "transactionType": "TDU",
				    "messageStatusCode": "",
				    "reserved3": "",
				    "traceNumber": "365111",
				    "pharmacyId": "P1",
				    "prescriptionNumber": "13147",
				    "additionalReference": "",
				    "rejectionCodes": "",
				    "reserved9": "",
				    "reserved10": ""
				  },
				  "ZZZSegments": [
				    { "transactionType": "TDU", "traceNumber": "365111", "pharmacyId": "P1", "prescriptionNumber": "13147" },
				    { "transactionType": "TAC", "traceNumber": "365111", "pharmacyId": "P1", "prescriptionNumber": "13147" }
				  ]
				}

										""";
	}

}
