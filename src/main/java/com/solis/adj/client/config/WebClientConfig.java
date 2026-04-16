package com.solis.adj.client.config;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

    private static final Logger log = LoggerFactory.getLogger(WebClientConfig.class);

    @Value("${gateway.api-key:}")
    private String gatewayApiKey;

    @Bean
    public WebClient webClient() {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(60));

        WebClient.Builder builder = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));

        if (gatewayApiKey != null && !gatewayApiKey.isBlank()) {
            String masked = gatewayApiKey.substring(0, 8) + "****";
            log.info("Gateway API key loaded from Secret Manager: {}... (masked)", masked);
            builder.defaultHeader("x-api-key", gatewayApiKey);
        } else {
            log.warn("Gateway API key is NOT set — outbound requests will not include x-api-key header");
        }

        return builder.build();
    }
}
