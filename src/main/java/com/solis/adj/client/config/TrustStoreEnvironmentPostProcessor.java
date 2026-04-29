package com.solis.adj.client.config;

import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Promotes gateway.ssl.trust-store* properties from the Spring Environment to JVM system
 * properties (javax.net.ssl.trustStore*) before any bean - and therefore any outbound HTTPS
 * call - is created. This lets operators configure the trust store once in
 * application-cloud.properties instead of passing -Djavax.net.ssl.trustStore=... at startup.
 */
public class TrustStoreEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(TrustStoreEnvironmentPostProcessor.class);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication app) {
        String path = env.getProperty("gateway.ssl.trust-store");
        if (path == null || path.isBlank()) {
            return;
        }
        if (!Files.exists(Path.of(path))) {
            log.warn("gateway.ssl.trust-store={} not found - skipping trustStore system property", path);
            return;
        }
        System.setProperty("javax.net.ssl.trustStore", path);
        String pwd = env.getProperty("gateway.ssl.trust-store-password");
        if (pwd != null) {
            System.setProperty("javax.net.ssl.trustStorePassword", pwd);
        }
        String type = env.getProperty("gateway.ssl.trust-store-type", "JKS");
        System.setProperty("javax.net.ssl.trustStoreType", type);
        log.info("Applied javax.net.ssl.trustStore={} (type={})", path, type);
    }
}
