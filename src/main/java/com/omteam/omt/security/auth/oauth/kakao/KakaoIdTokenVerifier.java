package com.omteam.omt.security.auth.oauth.kakao;

import com.omteam.omt.config.properties.OAuthProperties;
import com.omteam.omt.security.auth.oauth.common.AbstractIdTokenVerifier;
import io.jsonwebtoken.SigningKeyResolver;
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
    protected String getAudience() {
        return oAuthProperties.getKakao().getClientId();
    }

    @Override
    protected SigningKeyResolver getSigningKeyResolver() {
        return publicKeyGenerator;
    }
}
