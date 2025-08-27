package com.solis.adj.client.controller;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

public class TimeZoneChecker {

    public static void main(String[] args) {
        // Get default system timezone (should match MacBook's setting)
        ZoneId zoneId = ZoneId.systemDefault();
        //String zoneName = zoneId.getId();
        //TimeZone timeZone = TimeZone.getDefault();

        // Get current date and time in that timezone
        ZonedDateTime now = ZonedDateTime.now(zoneId);

        // Format date and time
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

        // Output
        //System.out.println("System Time Zone ID: " + zoneName);
        //System.out.println("Display Name       : " + timeZone.getDisplayName());
        //System.out.println("Raw Offset (ms)    : " + timeZone.getRawOffset());
        System.out.println("Current Date & Time: " + ZonedDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")));
        
		Instant nowd = Instant.now();
		String datetime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC).format(nowd);
		System.out.println("Instant Current Date & Time: " + datetime);
    }
}
