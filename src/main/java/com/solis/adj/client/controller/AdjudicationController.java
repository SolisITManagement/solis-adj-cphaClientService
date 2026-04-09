package com.solis.adj.client.controller;

import java.util.Iterator;

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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.solis.adj.client.service.CphaClientService;
import com.solis.adj.client.util.BCPayloadBuilder;

@Controller
public class AdjudicationController {

	private static final Logger log = LoggerFactory.getLogger(AdjudicationController.class);

	private final CphaClientService clientService;

	public AdjudicationController(CphaClientService clientService) {
		this.clientService = clientService;
	}

	@GetMapping("/jsononly")
	public String jsonOnly(Model model) {
		String datetime = BCPayloadBuilder.formatCurrentDateTime();
		String uid = BCPayloadBuilder.generateRandomId();
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
		String datetime = BCPayloadBuilder.formatCurrentDateTime();
		String uid = BCPayloadBuilder.generateRandomId();
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

}
