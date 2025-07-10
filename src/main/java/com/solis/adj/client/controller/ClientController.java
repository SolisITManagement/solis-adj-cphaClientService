package com.solis.adj.client.controller;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;

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
    private  String generateRandomId() {
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
		Instant now = Instant.now();
		String datetime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC).format(now);
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
				    "IIN": "123456",
				    "Version Number": "03",
				    "Transaction Code": "01",
				    "Provider Software ID": "XY",
				    "Provider Software Version": "01",
				    "Active Device ID": "DEVICE01",
				    "Pharmacy ID Code": "PHARM12345",
				    "Provider Transaction Date": "240512",
				    "Trace Number": "000123",
				    "Carrier ID": "AB",
				    "Group Number or Code": "GROUP1234",
				    "Client ID # or Code": "CLIENT123456789",
				    "Patient Code": "PC1",
				    "Patient DOB": "19900101",
				    "Cardholder Identity": "CH123",
				    "Relationship": "1",
				    "Patient First Name": "John",
				    "Patient Last Name": "Doe",
				    "Provincial Health Care ID Code": "PHCID1234567",
				    "Patient Gender": "M",
				    "Medical Reason Reference": "R",
				    "Medical Condition / Reason for Use": "REASON",
				    "New/Refill Code": "N",
				    "Original Prescription Number": "123456789",
				    "Refill / Repeat Authorizations": "02",
				    "Current Rx Number": "987654321",
				    "DIN /GP# / PIN": "12345678",
				    "SSC": "SSC",
				    "Quantity": "001000",
				    "Days Supply": "030",
				    "Prescriber ID Reference": "PR",
				    "Prescriber ID": "DRID123456",
				    "Product Selection": "1",
				    "Unlisted Compound": "0",
				    "Special Authorization Number or Code": "AUTH123",
				    "Intervention and Exception Codes": "INT1",
				    "Drug Cost / Product Value": "050000",
				    "Cost Upcharge": "1000",
				    "Professional Fee": "1500",
				    "Compounding Charge": "00500",
				    "Compounding Time": "30",
				    "Special Services Fee(s)": "02000",
				    "Previously Paid": "000000",
				    "Pharmacist ID": "PHM123",
				    "Adjudication Date": "250512"
				  }
				}
				""".formatted(uid, datetime);
		model.addAttribute("jsonData", defaultJson);
		return "jsononly";
	}


	@PostMapping("/adjudicate")
	public String submitJson(@RequestParam("jsonData") String jsonData, @RequestParam("action") String action, Model model) throws Exception {
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		System.out.println("incoming jsonData action: " + action + "\request: " + jsonData);
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
	    System.out.println("Adjudication processing took " + seconds + " ms");
	    ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = (ObjectNode) mapper.readTree(response);

        if (root.has("count")) { //10k claim - request object parsing
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
                    //ObjectNode requestNode = requestFixedStringToJson.parse(raw);
                    ObjectNode requestNode = requestFixedStringToJson.decodeFixedRequest(raw, mapper);
                    System.out.println("10k jsonData request: " + requestNode.toString());
                    telusResponse.set("Request", requestNode);
                }
            }
        }
		model.addAttribute("response", mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root));
		model.addAttribute("jsonData", jsonData);
		model.addAttribute("executionTime", seconds);
		return "jsononly";
	}
	
	@GetMapping("/total-and-details")
	public String TotalAndDetails(Model model) {
		Instant now = Instant.now();
		String datetime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC).format(now);
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
	
}
