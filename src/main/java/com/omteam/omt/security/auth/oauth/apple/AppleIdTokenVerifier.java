package com.omteam.omt.security.auth.oauth.apple;

import com.omteam.omt.config.properties.OAuthProperties;
import com.omteam.omt.security.auth.oauth.common.AbstractIdTokenVerifier;
import io.jsonwebtoken.Locator;
import java.security.Key;
import java.util.List;
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
    protected List<String> getAllowedAudiences() {
        return List.of(oAuthProperties.getApple().getClientId());
    }

    @Override
    protected Locator<Key> getKeyLocator() {
        return publicKeyGenerator;
    }
}
