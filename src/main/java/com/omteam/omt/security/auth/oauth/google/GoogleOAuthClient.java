package com.omteam.omt.security.auth.oauth.google;

import com.omteam.omt.security.auth.oauth.OAuthClient;
import com.omteam.omt.security.auth.oauth.OAuthUserInfo;
import com.omteam.omt.user.domain.SocialProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GoogleOAuthClient implements OAuthClient {

    private final GoogleIdTokenVerifier googleIdTokenVerifier;

    @Override
    public SocialProvider getProvider() {
        return SocialProvider.GOOGLE;
    }

    @Override
    public OAuthUserInfo getUserInfo(String idToken) {
        Claims claims = googleIdTokenVerifier.verify(idToken);
        return new GoogleUserInfo(claims);
    }
}
