package com.omteam.omt.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "oauth")
public class OAuthProperties {

    private Provider google = new Provider();
    private Provider kakao = new Provider();
    private AppleProvider apple = new AppleProvider();

    @Getter
    @Setter
    public static class Provider {
        private String clientId;
    }

    @Getter
    @Setter
    public static class AppleProvider extends Provider {
        private String publicKeyUri = "https://appleid.apple.com/auth/keys";
    }
}
