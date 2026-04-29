package com.solis.adj.client.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Transport client that posts NL Pharmacy Network (NLPN) test-client
 * payloads to the data-service NL publish endpoint. Uses the shared
 * {@link WebClient} bean which already injects the
 * {@code x-api-key} gateway header from Secret Manager.
 */
@Service
public class NLClientService {

	private static final Logger log = LoggerFactory.getLogger(NLClientService.class);

	private final WebClient webClient;
	private final String nlHl7EventUrl;

	public NLClientService(
			WebClient webClient,
			@Value("${target.dis.nl.hl7.event.url}") String nlHl7EventUrl) {
		this.webClient = webClient;
		this.nlHl7EventUrl = nlHl7EventUrl;
	}

	public String sendNlHl7Request(String jsonPayload) {
		log.info("Calling NL HL7 event service: {}", nlHl7EventUrl);
		return postJson(nlHl7EventUrl, jsonPayload, "NL HL7 event");
	}

	private String postJson(String url, String payload, String serviceName) {
		try {
			return webClient.post()
					.uri(url)
					.contentType(MediaType.APPLICATION_JSON)
					.bodyValue(payload)
					.retrieve()
					.bodyToMono(String.class)
					.block();
		} catch (WebClientResponseException e) {
			log.error("HTTP {} from {} service: {}", e.getStatusCode(), serviceName, e.getResponseBodyAsString(), e);
			return "{\"error\": \"HTTP " + e.getStatusCode().value() + " from " + serviceName + " service\"}";
		} catch (Exception e) {
			// Include the exception class name so transport-level failures (for example
			// java.net.BindException / AnnotatedConnectException "Can't assign requested
			// address", UnknownHostException, SSL handshake failures) are immediately
			// distinguishable from opaque error messages in both logs and the JSON body.
			String errorType = e.getClass().getSimpleName();
			log.error("Error calling {} service at {}: {}: {}", serviceName, url, errorType, e.getMessage(), e);
			return "{\"error\": \"Error calling " + serviceName + " service: " + errorType + ": " + e.getMessage() + "\"}";
		}
	}
}
