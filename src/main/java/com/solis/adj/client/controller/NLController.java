package com.solis.adj.client.controller;

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
import com.solis.adj.client.service.NLClientService;
import com.solis.adj.client.util.NLPayloadBuilder;

/**
 * NL Pharmacy Network (NLPN) test client controller. Mirrors the BC
 * pattern ({@link BCPharmaNetController}) but targets the NL data-service
 * endpoint and NL-specific Thymeleaf views (prefix {@code nl-}).
 */
@Controller
public class NLController {

	private static final Logger log = LoggerFactory.getLogger(NLController.class);

	private final NLClientService clientService;

	public NLController(NLClientService clientService) {
		this.clientService = clientService;
	}

	// ---- NL Pharmacy Network landing + GET endpoints ----

	@GetMapping("/ehealth-nl")
	public String ehealthNl() {
		return "ehealth-nl";
	}

	@GetMapping("/nl-add-patient-note")
	public String addPatientNote(Model model) {
		return serveNlPage(model, "nl-add-patient-note");
	}

	@GetMapping("/nl-deprecate-patient-note")
	public String deprecatePatientNote(Model model) {
		return serveNlPage(model, "nl-deprecate-patient-note");
	}

	@GetMapping("/nl-patient-note-query")
	public String patientNoteQuery(Model model) {
		return serveNlPage(model, "nl-patient-note-query");
	}

	@GetMapping("/nl-add-note-to-record")
	public String addNoteToRecord(Model model) {
		return serveNlPage(model, "nl-add-note-to-record");
	}

	private String serveNlPage(Model model, String viewName) {
		model.addAttribute("jsonData", NLPayloadBuilder.getBusinessDataDefault(viewName));
		return viewName;
	}

	// ---- NL Pharmacy Network POST ----

	@PostMapping("/callNlHl7Event")
	public String publishNlHl7Event(@RequestParam("jsonData") String jsonData,
			@RequestParam(value = "returnView") String returnView, Model model) {
		String safeView = NLPayloadBuilder.sanitizeViewName(returnView);
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		String fullPayload;
		String messageId = null;
		try {
			fullPayload = NLPayloadBuilder.buildFullPayload(jsonData, safeView);
			ObjectMapper payloadMapper = new ObjectMapper();
			messageId = payloadMapper.readTree(fullPayload).path("Header").path("messageId").asText(null);
		} catch (Exception e) {
			log.error("Failed to build NL full payload from business data", e);
			model.addAttribute("response", "{\"error\": \"Failed to build request: " + e.getMessage() + "\"}");
			model.addAttribute("jsonData", jsonData);
			return safeView;
		}

		log.info("Sending NL HL7 event request to Data Service (messageId={}, view={})", messageId, safeView);
		log.debug("Full NL payload: {}", fullPayload);
		String response = clientService.sendNlHl7Request(fullPayload);
		stopWatch.stop();
		double seconds = stopWatch.getTotalTimeMillis() / 1000.0;
		log.info("NL HL7 event processing took {} seconds", seconds);

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
			log.debug("Could not parse HL7 fields from NL response", e);
		}

		model.addAttribute("response", formattedResponse);
		model.addAttribute("hl7Request", hl7Request);
		model.addAttribute("hl7Response", hl7Response);
		model.addAttribute("jsonData", jsonData);
		model.addAttribute("messageId", messageId);
		model.addAttribute("executionTime", seconds);
		return safeView;
	}
}
