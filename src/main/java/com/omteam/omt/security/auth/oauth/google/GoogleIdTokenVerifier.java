package com.omteam.omt.security.auth.oauth.google;

import com.omteam.omt.config.properties.OAuthProperties;
import com.omteam.omt.security.auth.oauth.common.AbstractIdTokenVerifier;
import io.jsonwebtoken.Locator;
import java.security.Key;
import java.util.List;
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
    protected List<String> getAllowedAudiences() {
        return oAuthProperties.getGoogle().getAllClientIds();
    }

    @Override
    protected Locator<Key> getKeyLocator() {
        return publicKeyGenerator;
    }
}
