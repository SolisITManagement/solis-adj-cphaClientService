package com.solis.adj.client.controller;
import java.util.List;

import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class RequestFixedStringToJson {

	    public ObjectNode parse(String fixedString) throws Exception  {
	        JSONObject request = new JSONObject();

	        request.put("IIN", fixedString.substring(0, 6).trim());
	        request.put("Version Number", fixedString.substring(6, 8).trim());
	        request.put("Transaction Code", fixedString.substring(8, 10).trim());
	        request.put("Provider Software ID", fixedString.substring(10, 12).trim());
	        request.put("Provider Software Version", fixedString.substring(12, 14).trim());
	        request.put("Active Device ID", fixedString.substring(14, 22).trim());
	        request.put("Pharmacy ID Code", fixedString.substring(22, 32).trim());
	        request.put("Provider Transaction Date", fixedString.substring(32, 38).trim());
	        request.put("Trace Number", fixedString.substring(38, 44).trim());
	        request.put("Carrier ID", fixedString.substring(44, 46).trim());
	        request.put("Group Number or Code", fixedString.substring(46, 56).trim());
	        request.put("Client ID # or Code", fixedString.substring(56, 71).trim());
	        request.put("Patient Code", fixedString.substring(71, 74).trim());
	        request.put("Patient DOB", fixedString.substring(74, 82).trim());
	        request.put("Cardholder Identity", fixedString.substring(82, 87).trim());
	        request.put("Relationship", fixedString.substring(87, 88).trim());
	        request.put("Patient First Name", fixedString.substring(88, 100).trim());
	        request.put("Patient Last Name", fixedString.substring(100, 115).trim());
	        request.put("Provincial Health Care ID Code", fixedString.substring(115, 128).trim());
	        request.put("Patient Gender", fixedString.substring(128, 129).trim());
	        request.put("Medical Reason Reference", fixedString.substring(129, 130).trim());
	        request.put("Medical Condition / Reason for Use", fixedString.substring(130, 136).trim());
	        request.put("New/Refill Code", fixedString.substring(136, 137).trim());
	        request.put("Original Prescription Number", fixedString.substring(137, 146).trim());
	        request.put("Refill / Repeat Authorizations", fixedString.substring(146, 148).trim());
	        request.put("Current Rx Number", fixedString.substring(148, 157).trim());
	        request.put("DIN /GP# / PIN", fixedString.substring(157, 165).trim());
	        request.put("SSC", fixedString.substring(165, 168).trim());
	        request.put("Quantity", fixedString.substring(168, 174).trim());
	        request.put("Days Supply", fixedString.substring(174, 177).trim());
	        request.put("Prescriber ID Reference", fixedString.substring(177, 179).trim());
	        request.put("Prescriber ID", fixedString.substring(179, 189).trim());
	        request.put("Product Selection", fixedString.substring(189, 190).trim());
	        request.put("Unlisted Compound", fixedString.substring(190, 191).trim());
	        request.put("Special Authorization Number or Code", fixedString.substring(191, 199).trim());
	        request.put("Intervention and Exception Codes", fixedString.substring(199, 203).trim());
	        request.put("Drug Cost / Product Value", fixedString.substring(203, 209).trim());
	        request.put("Cost Upcharge", fixedString.substring(209, 214).trim());
	        request.put("Professional Fee", fixedString.substring(214, 219).trim());
	        request.put("Compounding Charge", fixedString.substring(219, 224).trim());
	        request.put("Compounding Time", fixedString.substring(224, 226).trim());
	        request.put("Special Services Fee(s)", fixedString.substring(226, 231).trim());
	        request.put("Previously Paid", fixedString.substring(231, 237).trim());
	        request.put("Pharmacist ID", fixedString.substring(237, 243).trim());
	        request.put("Adjudication Date", fixedString.substring(243, 249).trim());

	        ObjectMapper mapper = new ObjectMapper();
	        ObjectNode requestNode = (ObjectNode) mapper.readTree(request.toString());
	        return requestNode;
	    }
	    
	    public ObjectNode decodeFixedRequest(String fixed, ObjectMapper mapper) {
	        ObjectNode request = mapper.createObjectNode();

	        for (FieldSpec spec : fieldSpecs) {
	            String value = safeSubstring(fixed, spec.start, spec.start + spec.length).trim();
	            request.put(spec.key, value);
	        }

	        return request;
	    }
	    
	    private  class FieldSpec {
	        String key;
	        int start;
	        int length;

	        FieldSpec(String key, int start, int length) {
	            this.key = key;
	            this.start = start;
	            this.length = length;
	        }
	    }
	    
	    private List<FieldSpec> fieldSpecs = List.of(
	    				new FieldSpec("IIN", 0, 6),
	    		        new FieldSpec("Version Number", 6, 2),
	    		        new FieldSpec("Transaction Code", 8, 2),
	    		        new FieldSpec("Provider Software ID", 10, 2),
	    		        new FieldSpec("Provider Software Version", 12, 2),
	    		        new FieldSpec("Active Device ID", 14, 8),
	    		        new FieldSpec("Pharmacy ID Code", 22, 10),
	    		        new FieldSpec("Provider Transaction Date", 32, 6),
	    		        new FieldSpec("Trace Number", 38, 6),
	    		        new FieldSpec("Carrier ID", 44, 2),
	    		        new FieldSpec("Group Number or Code", 46, 10),
	    		        new FieldSpec("Client ID # or Code", 56, 15),
	    		        new FieldSpec("Patient Code", 71, 3),
	    		        new FieldSpec("Patient DOB", 74, 8),
	    		        new FieldSpec("Cardholder Identity", 82, 5),
	    		        new FieldSpec("Relationship", 87, 1),
	    		        new FieldSpec("Patient First Name", 88, 12),
	    		        new FieldSpec("Patient Last Name", 100, 15),
	    		        new FieldSpec("Provincial Health Care ID Code", 115, 13),
	    		        new FieldSpec("Patient Gender", 128, 1),
	    		        new FieldSpec("Medical Reason Reference", 129, 1),
	    		        new FieldSpec("Medical Condition / Reason for Use", 130, 6),
	    		        new FieldSpec("New/Refill Code", 136, 1),
	    		        new FieldSpec("Original Prescription Number", 137, 9),
	    		        new FieldSpec("Refill / Repeat Authorizations", 146, 2),
	    		        new FieldSpec("Current Rx Number", 148, 9),
	    		        new FieldSpec("DIN /GP# / PIN", 157, 8),
	    		        new FieldSpec("SSC", 165, 3),
	    		        new FieldSpec("Quantity", 168, 6),
	    		        new FieldSpec("Days Supply", 174, 3),
	    		        new FieldSpec("Prescriber ID Reference", 177, 2),
	    		        new FieldSpec("Prescriber ID", 179, 10),
	    		        new FieldSpec("Product Selection", 189, 1),
	    		        new FieldSpec("Unlisted Compound", 190, 1),
	    		        new FieldSpec("Special Authorization Number or Code", 191, 8),
	    		        new FieldSpec("Intervention and Exception Codes", 199, 4),
	    		        new FieldSpec("Drug Cost / Product Value", 203, 6),
	    		        new FieldSpec("Cost Upcharge", 209, 5),
	    		        new FieldSpec("Professional Fee", 214, 5),
	    		        new FieldSpec("Compounding Charge", 219, 5),
	    		        new FieldSpec("Compounding Time", 224, 2),
	    		        new FieldSpec("Special Services Fee(s)", 226, 5),
	    		        new FieldSpec("Previously Paid", 231, 6),
	    		        new FieldSpec("Pharmacist ID", 237, 6),
	    		        new FieldSpec("Adjudication Date", 243, 6)
	    			);

	    
	    private String safeSubstring(String str, int start, int end) {
	        if (start >= str.length()) return "";
	        return str.substring(start, Math.min(str.length(), end));
	    }
	}


