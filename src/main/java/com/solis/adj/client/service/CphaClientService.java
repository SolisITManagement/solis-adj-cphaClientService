package com.solis.adj.client.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class CphaClientService {

    @Value("${target.service.url}")
    private String targetUrl;

    private final WebClient webClient = WebClient.create();

    public String sendJsonToPublisher(String jsonPayload) {
        return webClient.post()
                .uri(targetUrl)
                .bodyValue(jsonPayload)
                .retrieve()
                .bodyToMono(String.class)
                .onErrorReturn("Error calling service")
                .block();
    }
}
