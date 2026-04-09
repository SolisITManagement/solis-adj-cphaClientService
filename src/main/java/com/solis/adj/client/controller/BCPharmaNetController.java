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
import com.solis.adj.client.service.CphaClientService;
import com.solis.adj.client.util.BCPayloadBuilder;

@Controller
public class BCPharmaNetController {

	private static final Logger log = LoggerFactory.getLogger(BCPharmaNetController.class);

	private final CphaClientService clientService;

	public BCPharmaNetController(CphaClientService clientService) {
		this.clientService = clientService;
	}

	// ---- BC PharmaNet GET endpoints ----

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

	@GetMapping("/bc-adj-reconciliation")
	public String adjReconciliation(Model model) { return serveBcPage(model, "bc-adj-reconciliation"); }

	@GetMapping("/bc-protective-word")
	public String protectiveWord(Model model) { return serveBcPage(model, "bc-protective-word"); }

	@GetMapping("/bc-keyword-verify")
	public String keywordVerify(Model model) { return serveBcPage(model, "bc-keyword-verify"); }

	@GetMapping("/bc-medication-update")
	public String medicationUpdate(Model model) { return serveBcPage(model, "bc-medication-update"); }

	@GetMapping("/bc-medication-update-reversal")
	public String medicationUpdateReversal(Model model) { return serveBcPage(model, "bc-medication-update-reversal"); }

	@GetMapping("/bc-phn-assignment")
	public String phnAssignment(Model model) { return serveBcPage(model, "bc-phn-assignment"); }

	@GetMapping("/bc-patient-name-search")
	public String patientNameSearch(Model model) { return serveBcPage(model, "bc-patient-name-search"); }

	private String serveBcPage(Model model, String viewName) {
		model.addAttribute("jsonData", BCPayloadBuilder.getBusinessDataDefault());
		return viewName;
	}

	// ---- BC PharmaNet POST ----

	@PostMapping("/callHl7Event")
	public String publishHl7Event(@RequestParam("jsonData") String jsonData,
			@RequestParam(value = "returnView") String returnView, Model model) {
		String safeView = BCPayloadBuilder.sanitizeViewName(returnView);
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		String fullPayload;
		String messageId = null;
		try {
			fullPayload = BCPayloadBuilder.buildFullPayload(jsonData, safeView);
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

}
