package com.omteam.omt.security.auth.oauth.apple;

import com.omteam.omt.security.auth.oauth.OAuthClient;
import com.omteam.omt.security.auth.oauth.OAuthUserInfo;
import com.omteam.omt.user.domain.SocialProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppleOAuthClient implements OAuthClient {

    private final AppleIdTokenVerifier appleIdTokenVerifier;

    @Override
    public SocialProvider getProvider() {
        return SocialProvider.APPLE;
    }

    @Override
    public OAuthUserInfo getUserInfo(String idToken) {
        Claims claims = appleIdTokenVerifier.verify(idToken);
        return new AppleUserInfo(claims);
    }
}
