package com.solis.adj.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CphaClientApplication {

    public static void main(String[] args) {
        // Force the JVM to use the IPv4 stack for all sockets. Must be set BEFORE Spring
        // boots, because java.net.InetAddress / SocketChannel read this property during
        // their static initialization and will not re-read it later (setting it via an
        // EnvironmentPostProcessor is too late on some JDKs).
        //
        // Why this exists: outbound HTTPS calls from this service go to the Cloud LB at
        // a literal IPv4 address (e.g. 34.98.75.115). On hosts where the JVM defaults
        // to IPv6-preferred mode and the kernel cannot supply a routable IPv6 source
        // address for that IPv4 destination, Reactor Netty's connect() fails with
        // "java.net.BindException: Can't assign requested address" (EADDRNOTAVAIL),
        // even though plain curl works from the same machine. Pinning to IPv4 makes
        // behaviour identical on local laptops, Cloud Run and GKE - no JVM flags or
        // start scripts required.
        System.setProperty("java.net.preferIPv4Stack", "true");

        SpringApplication.run(CphaClientApplication.class, args);
    }
}
