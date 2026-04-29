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

	@GetMapping("/nl-retract-action")
	public String retractAction(Model model) {
		return serveNlPage(model, "nl-retract-action");
	}

	@GetMapping("/nl-issue-mgmt-override")
	public String issueMgmtOverride(Model model) {
		return serveNlPage(model, "nl-issue-mgmt-override");
	}

	@GetMapping("/nl-refusal-to-dispense")
	public String refusalToDispense(Model model) {
		return serveNlPage(model, "nl-refusal-to-dispense");
	}

	@GetMapping("/nl-dispense-transfer")
	public String dispenseTransfer(Model model) {
		return serveNlPage(model, "nl-dispense-transfer");
	}

	@GetMapping("/nl-activate-rx")
	public String activateRx(Model model) {
		return serveNlPage(model, "nl-activate-rx");
	}

	@GetMapping("/nl-activate-device-rx")
	public String activateDeviceRx(Model model) {
		return serveNlPage(model, "nl-activate-device-rx");
	}

	@GetMapping("/nl-suspend-rx")
	public String suspendRx(Model model) {
		return serveNlPage(model, "nl-suspend-rx");
	}

	@GetMapping("/nl-resume-rx")
	public String resumeRx(Model model) {
		return serveNlPage(model, "nl-resume-rx");
	}

	@GetMapping("/nl-abort-dispense-auth")
	public String abortDispenseAuth(Model model) {
		return serveNlPage(model, "nl-abort-dispense-auth");
	}

	@GetMapping("/nl-abort-rx")
	public String abortRx(Model model) {
		return serveNlPage(model, "nl-abort-rx");
	}

	@GetMapping("/nl-record-dispense")
	public String recordDispense(Model model) {
		return serveNlPage(model, "nl-record-dispense");
	}

	@GetMapping("/nl-record-device-dispense")
	public String recordDeviceDispense(Model model) {
		return serveNlPage(model, "nl-record-device-dispense");
	}

	@GetMapping("/nl-record-pickup")
	public String recordPickup(Model model) {
		return serveNlPage(model, "nl-record-pickup");
	}

	@GetMapping("/nl-record-supply-event")
	public String recordSupplyEvent(Model model) {
		return serveNlPage(model, "nl-record-supply-event");
	}

	@GetMapping("/nl-record-dispense-reversal")
	public String recordDispenseReversal(Model model) {
		return serveNlPage(model, "nl-record-dispense-reversal");
	}

	// ── Section 14: Other Medication / OTC ──

	@GetMapping("/nl-record-other-med")
	public String recordOtherMed(Model model) {
		return serveNlPage(model, "nl-record-other-med");
	}

	@GetMapping("/nl-update-other-med")
	public String updateOtherMed(Model model) {
		return serveNlPage(model, "nl-update-other-med");
	}

	// ── Section 16: Consent ──

	@GetMapping("/nl-record-consent")
	public String recordConsent(Model model) {
		return serveNlPage(model, "nl-record-consent");
	}

	// ── Section 17: Adverse Reactions ──

	@GetMapping("/nl-record-adverse-reaction")
	public String recordAdverseReaction(Model model) {
		return serveNlPage(model, "nl-record-adverse-reaction");
	}

	@GetMapping("/nl-update-adverse-reaction")
	public String updateAdverseReaction(Model model) {
		return serveNlPage(model, "nl-update-adverse-reaction");
	}

	// ── Section 18: Allergy / Intolerance ──

	@GetMapping("/nl-add-allergy")
	public String addAllergy(Model model) {
		return serveNlPage(model, "nl-add-allergy");
	}

	@GetMapping("/nl-update-allergy")
	public String updateAllergy(Model model) {
		return serveNlPage(model, "nl-update-allergy");
	}

	// ── Section 19: Medical Conditions ──

	@GetMapping("/nl-record-medical-condition")
	public String recordMedicalCondition(Model model) {
		return serveNlPage(model, "nl-record-medical-condition");
	}

	@GetMapping("/nl-update-medical-condition")
	public String updateMedicalCondition(Model model) {
		return serveNlPage(model, "nl-update-medical-condition");
	}

	// ── Section 20: Professional Services ──

	@GetMapping("/nl-record-prof-service")
	public String recordProfService(Model model) {
		return serveNlPage(model, "nl-record-prof-service");
	}

	// ── Section 21: Basic Observations ──

	@GetMapping("/nl-record-basic-observation")
	public String recordBasicObservation(Model model) {
		return serveNlPage(model, "nl-record-basic-observation");
	}

	// ── Section 15: Medication / Device Queries ──

	@GetMapping("/nl-query-dev-disp-detail")
	public String queryDevDispDetail(Model model) {
		return serveNlPage(model, "nl-query-dev-disp-detail");
	}

	@GetMapping("/nl-query-dev-disp-summary")
	public String queryDevDispSummary(Model model) {
		return serveNlPage(model, "nl-query-dev-disp-summary");
	}

	@GetMapping("/nl-query-dev-rx-detail")
	public String queryDevRxDetail(Model model) {
		return serveNlPage(model, "nl-query-dev-rx-detail");
	}

	@GetMapping("/nl-query-dev-disp-by-rx")
	public String queryDevDispByRx(Model model) {
		return serveNlPage(model, "nl-query-dev-disp-by-rx");
	}

	@GetMapping("/nl-query-dev-rx-summary")
	public String queryDevRxSummary(Model model) {
		return serveNlPage(model, "nl-query-dev-rx-summary");
	}

	@GetMapping("/nl-query-med-rx-detail")
	public String queryMedRxDetail(Model model) {
		return serveNlPage(model, "nl-query-med-rx-detail");
	}

	@GetMapping("/nl-query-med-disp-detail")
	public String queryMedDispDetail(Model model) {
		return serveNlPage(model, "nl-query-med-disp-detail");
	}

	@GetMapping("/nl-query-med-disp-summary")
	public String queryMedDispSummary(Model model) {
		return serveNlPage(model, "nl-query-med-disp-summary");
	}

	@GetMapping("/nl-query-med-rx-detail-rx")
	public String queryMedRxDetailRx(Model model) {
		return serveNlPage(model, "nl-query-med-rx-detail-rx");
	}

	@GetMapping("/nl-query-med-disp-by-rx")
	public String queryMedDispByRx(Model model) {
		return serveNlPage(model, "nl-query-med-disp-by-rx");
	}

	@GetMapping("/nl-query-med-rx-summary")
	public String queryMedRxSummary(Model model) {
		return serveNlPage(model, "nl-query-med-rx-summary");
	}

	@GetMapping("/nl-query-med-profile-generic")
	public String queryMedProfileGeneric(Model model) {
		return serveNlPage(model, "nl-query-med-profile-generic");
	}

	@GetMapping("/nl-query-med-profile-detail")
	public String queryMedProfileDetail(Model model) {
		return serveNlPage(model, "nl-query-med-profile-detail");
	}

	@GetMapping("/nl-query-med-profile-summary")
	public String queryMedProfileSummary(Model model) {
		return serveNlPage(model, "nl-query-med-profile-summary");
	}

	@GetMapping("/nl-query-other-med")
	public String queryOtherMed(Model model) {
		return serveNlPage(model, "nl-query-other-med");
	}

	@GetMapping("/nl-query-remaining-fill")
	public String queryRemainingFill(Model model) {
		return serveNlPage(model, "nl-query-remaining-fill");
	}

	@GetMapping("/nl-query-unfilled-rx")
	public String queryUnfilledRx(Model model) {
		return serveNlPage(model, "nl-query-unfilled-rx");
	}

	// ── Section 17 / 18 / 19 / 20 / 21 Query siblings ──

	@GetMapping("/nl-query-adverse-reactions")
	public String queryAdverseReactions(Model model) {
		return serveNlPage(model, "nl-query-adverse-reactions");
	}

	@GetMapping("/nl-query-allergies")
	public String queryAllergies(Model model) {
		return serveNlPage(model, "nl-query-allergies");
	}

	@GetMapping("/nl-query-medical-conditions")
	public String queryMedicalConditions(Model model) {
		return serveNlPage(model, "nl-query-medical-conditions");
	}

	@GetMapping("/nl-query-condition-history")
	public String queryConditionHistory(Model model) {
		return serveNlPage(model, "nl-query-condition-history");
	}

	@GetMapping("/nl-query-prof-services")
	public String queryProfServices(Model model) {
		return serveNlPage(model, "nl-query-prof-services");
	}

	@GetMapping("/nl-query-basic-observations")
	public String queryBasicObservations(Model model) {
		return serveNlPage(model, "nl-query-basic-observations");
	}

	// ── Section 22: Client Registry ──

	@GetMapping("/nl-cr-find-candidates")
	public String crFindCandidates(Model model) {
		return serveNlPage(model, "nl-cr-find-candidates");
	}

	@GetMapping("/nl-cr-get-demographics")
	public String crGetDemographics(Model model) {
		return serveNlPage(model, "nl-cr-get-demographics");
	}

	@GetMapping("/nl-cr-add-person")
	public String crAddPerson(Model model) {
		return serveNlPage(model, "nl-cr-add-person");
	}

	@GetMapping("/nl-cr-revise-person")
	public String crRevisePerson(Model model) {
		return serveNlPage(model, "nl-cr-revise-person");
	}

	// ── Section 23: Polling / Broadcast / Password ──

	@GetMapping("/nl-poll-request")
	public String pollRequest(Model model) {
		return serveNlPage(model, "nl-poll-request");
	}

	@GetMapping("/nl-poll-fetch-next")
	public String pollFetchNext(Model model) {
		return serveNlPage(model, "nl-poll-fetch-next");
	}

	@GetMapping("/nl-poll-exception")
	public String pollException(Model model) {
		return serveNlPage(model, "nl-poll-exception");
	}

	@GetMapping("/nl-broadcast-topics")
	public String broadcastTopics(Model model) {
		return serveNlPage(model, "nl-broadcast-topics");
	}

	@GetMapping("/nl-broadcast-subscribe")
	public String broadcastSubscribe(Model model) {
		return serveNlPage(model, "nl-broadcast-subscribe");
	}

	@GetMapping("/nl-update-password")
	public String updatePassword(Model model) {
		return serveNlPage(model, "nl-update-password");
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
