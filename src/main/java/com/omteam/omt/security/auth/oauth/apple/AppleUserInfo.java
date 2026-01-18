package com.omteam.omt.security.auth.oauth.apple;

import com.omteam.omt.security.auth.oauth.OAuthUserInfo;
import io.jsonwebtoken.Claims;

public class AppleUserInfo implements OAuthUserInfo {

    private final Claims claims;

    public AppleUserInfo(Claims claims) {
        this.claims = claims;
    }

    @Override
    public String getProviderUserId() {
        return claims.getSubject(); // sub
    }

    @Override
    public String getEmail() {
        return claims.get("email", String.class); // 최초 로그인만 존재
    }
}

