package com.omteam.omt.security.auth.oauth.google;

import com.omteam.omt.config.properties.OAuthProperties;
import com.omteam.omt.security.auth.oauth.common.AbstractIdTokenVerifier;
import io.jsonwebtoken.SigningKeyResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GoogleIdTokenVerifier extends AbstractIdTokenVerifier {

    private static final String ISSUER = "https://accounts.google.com";

    private final GooglePublicKeyGenerator publicKeyGenerator;
    private final OAuthProperties oAuthProperties;

    @Override
    protected String getIssuer() {
        return ISSUER;
    }

    @Override
    protected String getAudience() {
        return oAuthProperties.getGoogle().getClientId();
    }

    @Override
    protected SigningKeyResolver getSigningKeyResolver() {
        return publicKeyGenerator;
    }
}
