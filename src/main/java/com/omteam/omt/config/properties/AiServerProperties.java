package com.omteam.omt.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ai-server")
public class AiServerProperties {

    private String baseUrl;
    private int timeoutSeconds = 10;
}
