package com.solis.adj.client.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
public class CphaClientService {

    private static final Logger log = LoggerFactory.getLogger(CphaClientService.class);

    private final WebClient webClient;
    private final String adjudicatorPublisherUrl;
    private final String totalsPublisherUrl;
    private final String adjRetryUrl;
    private final String bcHl7EventUrl;

    public CphaClientService(
            WebClient webClient,
            @Value("${target.adjudicate.service.url}") String adjudicatorPublisherUrl,
            @Value("${target.totals.service.url}") String totalsPublisherUrl,
            @Value("${target.adjudicate.service.retry.url}") String adjRetryUrl,
            @Value("${target.dis.bc.hl7.event.url}") String bcHl7EventUrl) {
        this.webClient = webClient;
        this.adjudicatorPublisherUrl = adjudicatorPublisherUrl;
        this.totalsPublisherUrl = totalsPublisherUrl;
        this.adjRetryUrl = adjRetryUrl;
        this.bcHl7EventUrl = bcHl7EventUrl;
    }

    public String sendJsonToPublisher(String jsonPayload) {
        log.info("Calling adjudication publish service: {}", adjudicatorPublisherUrl);
        return postJson(adjudicatorPublisherUrl, jsonPayload, "adjudication publish");
    }

    public String sendJsonPublishTotals(String jsonPayload) {
        log.info("Calling totals service: {}", totalsPublisherUrl);
        return postJson(totalsPublisherUrl, jsonPayload, "totals");
    }

    public String retry(String jsonPayload) {
        log.info("Calling adjudication retry service: {}", adjRetryUrl);
        return postJson(adjRetryUrl, jsonPayload, "adjudication retry");
    }

    public String sendHL7Request(String jsonPayload) {
        log.info("Calling BC HL7 event service: {}", bcHl7EventUrl);
        return postJson(bcHl7EventUrl, jsonPayload, "BC HL7 event");
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
