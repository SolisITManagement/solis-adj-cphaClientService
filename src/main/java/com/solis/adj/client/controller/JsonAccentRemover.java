package com.solis.adj.client.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.text.Normalizer;
import java.util.regex.Pattern;

public class JsonAccentRemover {

    public String removeAccentsFromNameFields(String json) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);

        JsonNode requestNode = root.path("Request");
        if (requestNode.isObject()) {
            ObjectNode requestObject = (ObjectNode) requestNode;

            // Remove accents from Patient First Name
            String firstName = requestObject.path("Patient First Name").asText(null);
            if (firstName != null) {
                requestObject.put("Patient First Name", removeAccents(firstName));
            }

            // Remove accents from Patient Last Name
            String lastName = requestObject.path("Patient Last Name").asText(null);
            if (lastName != null) {
                requestObject.put("Patient Last Name", removeAccents(lastName));
            }
        }

        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    }

    private String removeAccents(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(normalized).replaceAll("");
    }

    // For testing
    public static void main(String[] args) throws Exception {
        String jsonInput = """
        		{
  "Header": {
    "UID": "rbGylItXbhGwXsn7e7UD",
    "Store Number": "203",
    "Type": "request",
    "Format": "cpha",
    "Source": "client",
    "Date": "2025-08-05 17:19:01"
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
    "Patient Last Name": "café",
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

        		""";
        JsonAccentRemover remover = new JsonAccentRemover();
        String result = remover.removeAccentsFromNameFields(jsonInput);
        System.out.println(result);
    }
}

