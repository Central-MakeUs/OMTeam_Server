package com.omteam.omt.config.properties;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
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
    private KakaoProvider kakao = new KakaoProvider();
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
            return Stream.of(iosClientId, androidClientId)
                    .filter(Objects::nonNull)
                    .filter(id -> !id.isEmpty())
                    .toList();
        }
    }

    @Getter
    @Setter
    public static class KakaoProvider {
        private String clientId;
        private String testClientId;

        public List<String> getAllClientIds() {
            return Stream.of(clientId, testClientId)
                    .filter(Objects::nonNull)
                    .filter(id -> !id.isEmpty())
                    .toList();
        }
    }

    @Getter
    @Setter
    public static class AppleProvider extends Provider {
        private String publicKeyUri = "https://appleid.apple.com/auth/keys";
    }
}
