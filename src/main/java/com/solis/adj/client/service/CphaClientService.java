package com.solis.adj.client.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class CphaClientService {

    @Value("${target.adjudicate.service.url}")
    private String adjudicatorPublisherUrl;
    
    @Value("${target.totals.service.url}")
    private String totalsPublisherUrl;
    
    @Value("${target.adjudicate.service.retry.url}")
    private String adjRetryUrl;
    
    @Value("${target.dis.bc.hl7.event.url}")
    private String bcHl7EventUrl;

    private final WebClient webClient = WebClient.create();

    public String sendJsonToPublisher(String jsonPayload) {
    	System.out.println("calling Data service: " + adjudicatorPublisherUrl);
        return webClient.post()
                .uri(adjudicatorPublisherUrl)
                .bodyValue(jsonPayload)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorReturn("Error calling service")
                .block();
    }

	public String sendJsonPublishTotals(String jsonPayload) {
		System.out.println("calling Data service: " + totalsPublisherUrl);
        return webClient.post()
                .uri(totalsPublisherUrl)
                .bodyValue(jsonPayload)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorReturn("Error calling Adjudication.. timed-out!!")
                .block();
	}
	
	public String retry(String jsonPayload) {
		System.out.println("calling Data service: " + adjRetryUrl);
        return webClient.post()
                .uri(adjRetryUrl)
                .bodyValue(jsonPayload)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorReturn("Error calling Adjudication.. timed-out!!")
                .block();
	}

	public String sendHL7Request(String jsonPayload) {
		System.out.println("calling Data service: " + bcHl7EventUrl);
        return webClient.post()
                .uri(bcHl7EventUrl)
                .bodyValue(jsonPayload)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorReturn("Error calling Find Canadidate.. timed-out!!")
                .block();
	}

	
}
