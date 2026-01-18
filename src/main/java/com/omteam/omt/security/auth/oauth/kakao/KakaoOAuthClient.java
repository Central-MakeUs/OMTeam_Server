package com.omteam.omt.security.auth.oauth.kakao;

import com.omteam.omt.security.auth.oauth.OAuthClient;
import com.omteam.omt.security.auth.oauth.OAuthUserInfo;
import com.omteam.omt.user.domain.SocialProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KakaoOAuthClient implements OAuthClient {

    private final KakaoIdTokenVerifier kakaoIdTokenVerifier;

    @Override
    public SocialProvider getProvider() {
        return SocialProvider.KAKAO;
    }

    @Override
    public OAuthUserInfo getUserInfo(String idToken) {
        Claims claims = kakaoIdTokenVerifier.verify(idToken);
        return new KakaoUserInfo(claims);
    }
}
