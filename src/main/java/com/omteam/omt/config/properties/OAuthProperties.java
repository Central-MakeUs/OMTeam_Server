package com.omteam.omt.config.properties;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "oauth")
public class OAuthProperties {

    private GoogleProvider google = new GoogleProvider();
    private Provider kakao = new Provider();
    private AppleProvider apple = new AppleProvider();

    @Getter
    @Setter
    public static class Provider {
        private String clientId;
    }

    @Getter
    @Setter
    public static class GoogleProvider {
        private String iosClientId;
        private String androidClientId;

        public List<String> getAllClientIds() {
            List<String> clientIds = new ArrayList<>();
            if (iosClientId != null && !iosClientId.isEmpty()) {
                clientIds.add(iosClientId);
            }
            if (androidClientId != null && !androidClientId.isEmpty()) {
                clientIds.add(androidClientId);
            }
            return clientIds;
        }
    }

    @Getter
    @Setter
    public static class AppleProvider extends Provider {
        private String publicKeyUri = "https://appleid.apple.com/auth/keys";
    }
}
