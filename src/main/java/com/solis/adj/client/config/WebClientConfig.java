package com.solis.adj.client.config;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.resolver.ResolvedAddressTypes;
import io.netty.resolver.dns.DnsAddressResolverGroup;
import io.netty.resolver.dns.DnsNameResolverBuilder;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

    private static final Logger log = LoggerFactory.getLogger(WebClientConfig.class);

    @Value("${gateway.api-key:}")
    private String gatewayApiKey;

    @Bean
    public WebClient webClient() {
        // Defence-in-depth for the outbound "Can't assign requested address" class of
        // failures: pin Netty's DNS resolver to IPv4_ONLY so every downstream call
        // (Cloud LB, retry endpoint, totals publisher, NL HL7 publisher) is made over
        // IPv4 regardless of how the JVM was launched. This complements the
        // java.net.preferIPv4Stack=true property set in CphaClientApplication.main(),
        // so the service keeps working even if that flag is ever ignored by a future
        // JDK / Netty native transport, or if another entry point into the JVM bypasses
        // main().
        DnsAddressResolverGroup ipv4Resolver = new DnsAddressResolverGroup(
                new DnsNameResolverBuilder()
                        .channelType(NioDatagramChannel.class)
                        .resolvedAddressTypes(ResolvedAddressTypes.IPV4_ONLY));

        HttpClient httpClient = HttpClient.create()
                .resolver(ipv4Resolver)
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
