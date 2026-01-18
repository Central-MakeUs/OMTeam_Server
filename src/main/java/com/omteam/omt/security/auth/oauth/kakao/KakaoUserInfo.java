package com.omteam.omt.security.auth.oauth.kakao;

import com.omteam.omt.security.auth.oauth.OAuthUserInfo;
import io.jsonwebtoken.Claims;

public class KakaoUserInfo implements OAuthUserInfo {

    private final Claims claims;

    public KakaoUserInfo(Claims claims) {
        this.claims = claims;
    }

    @Override
    public String getProviderUserId() {
        return claims.getSubject();
    }

    @Override
    public String getEmail() {
        return claims.get("email", String.class);
    }
}
