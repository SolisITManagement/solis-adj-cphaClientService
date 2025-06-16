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
                .onErrorReturn("Error calling service")
                .block();
	}
}
