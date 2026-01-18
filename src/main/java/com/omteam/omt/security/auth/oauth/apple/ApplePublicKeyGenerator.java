package com.omteam.omt.security.auth.oauth.apple;

import com.omteam.omt.security.auth.oauth.common.AbstractPublicKeyGenerator;
import org.springframework.stereotype.Component;

@Component
public class ApplePublicKeyGenerator extends AbstractPublicKeyGenerator {

    private static final String JWKS_URL = "https://appleid.apple.com/auth/keys";

    @Override
    protected String getJwksUrl() {
        return JWKS_URL;
    }

    @Override
    protected String getProviderName() {
        return "Apple";
    }
}
