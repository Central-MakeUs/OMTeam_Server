package com.omteam.omt.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

@Configuration
public class TimeZoneConfig {

    private static final String DEFAULT_TIMEZONE = "Asia/Seoul";

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone(DEFAULT_TIMEZONE));
    }
}
