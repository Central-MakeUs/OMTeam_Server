package com.omteam.omt.security.auth.oauth.apple;

import com.omteam.omt.config.properties.OAuthProperties;
import com.omteam.omt.security.auth.oauth.common.AbstractIdTokenVerifier;
import io.jsonwebtoken.SigningKeyResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppleIdTokenVerifier extends AbstractIdTokenVerifier {

    private static final String ISSUER = "https://appleid.apple.com";

    private final ApplePublicKeyGenerator publicKeyGenerator;
    private final OAuthProperties oAuthProperties;

    @Override
    protected String getIssuer() {
        return ISSUER;
    }

    @Override
    protected String getAudience() {
        return oAuthProperties.getApple().getClientId();
    }

    @Override
    protected SigningKeyResolver getSigningKeyResolver() {
        return publicKeyGenerator;
    }
}
