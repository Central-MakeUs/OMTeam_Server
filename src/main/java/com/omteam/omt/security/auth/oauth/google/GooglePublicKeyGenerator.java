package com.omteam.omt.security.auth.oauth.google;

import com.omteam.omt.security.auth.oauth.common.AbstractPublicKeyGenerator;
import org.springframework.stereotype.Component;

@Component
public class GooglePublicKeyGenerator extends AbstractPublicKeyGenerator {

    private static final String JWKS_URL = "https://www.googleapis.com/oauth2/v3/certs";

    @Override
    protected String getJwksUrl() {
        return JWKS_URL;
    }

    @Override
    protected String getProviderName() {
        return "Google";
    }
}
