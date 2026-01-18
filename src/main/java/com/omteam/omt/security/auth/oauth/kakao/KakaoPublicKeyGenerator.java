package com.omteam.omt.security.auth.oauth.kakao;

import com.omteam.omt.security.auth.oauth.common.AbstractPublicKeyGenerator;
import org.springframework.stereotype.Component;

@Component
public class KakaoPublicKeyGenerator extends AbstractPublicKeyGenerator {

    private static final String JWKS_URL = "https://kauth.kakao.com/.well-known/jwks.json";

    @Override
    protected String getJwksUrl() {
        return JWKS_URL;
    }

    @Override
    protected String getProviderName() {
        return "Kakao";
    }
}
