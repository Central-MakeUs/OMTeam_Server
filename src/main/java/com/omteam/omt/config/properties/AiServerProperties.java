package com.omteam.omt.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ai-server")
@Getter
@Setter
public class AiServerProperties {
    private String baseUrl;
    private int timeoutSeconds = 30;
}
