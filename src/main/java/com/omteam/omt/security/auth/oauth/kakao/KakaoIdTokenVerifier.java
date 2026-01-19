package com.omteam.omt.security.auth.oauth.kakao;

import com.omteam.omt.config.properties.OAuthProperties;
import com.omteam.omt.security.auth.oauth.common.AbstractIdTokenVerifier;
import io.jsonwebtoken.Locator;
import java.security.Key;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KakaoIdTokenVerifier extends AbstractIdTokenVerifier {

    private static final String ISSUER = "https://kauth.kakao.com";

    private final KakaoPublicKeyGenerator publicKeyGenerator;
    private final OAuthProperties oAuthProperties;

    @Override
    protected String getIssuer() {
        return ISSUER;
    }

    @Override
    protected List<String> getAllowedAudiences() {
        return List.of(oAuthProperties.getKakao().getClientId());
    }

    @Override
    protected Locator<Key> getKeyLocator() {
        return publicKeyGenerator;
    }
}
