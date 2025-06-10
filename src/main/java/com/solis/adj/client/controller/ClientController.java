package com.solis.adj.client.controller;

import com.solis.adj.client.service.CphaClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class ClientController {

	@Autowired
	private CphaClientService clientService;

	@GetMapping("/")
	public String index() {
		return "index";
	}

	@GetMapping("/jsononly")
	public String jsonOnly(Model model) {
		String defaultJson = """
				{
				  "Header": {
				    "UID": "Lakhbir-1",
				    "Store Number": "203",
				    "Type": "request",
				    "Format": "cpha",
				    "Source": "client",
				    "Date": "2025-06-09 20:38:23"
				  },
				  "A": {
				    "IIN": "123456",
				    "Version Number": "03",
				    "Transaction Code": "01",
				    "Provider Software ID": "XY",
				    "Provider Software Version": "01",
				    "Active Device ID": "DEVICE01"
				  },
				  "B": {
				    "Pharmacy ID Code": "PHARM12345",
				    "Provider Transaction Date": "240512",
				    "Trace Number": "000123"
				  },
				  "C": {
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
				    "Patient Gender": "M"
				  },
				  "D": {
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
		model.addAttribute("jsonData", defaultJson);
		return "jsononly";
	}


	@PostMapping("/adjudicate")
	public String submitJson(@RequestParam("jsonData") String jsonData, Model model) {
		System.out.println("incoming jsoData: " + jsonData);
		String response = clientService.sendJsonToPublisher(jsonData);
		model.addAttribute("response", response);
		model.addAttribute("jsonData", jsonData);
		return "jsononly";
	}
}
