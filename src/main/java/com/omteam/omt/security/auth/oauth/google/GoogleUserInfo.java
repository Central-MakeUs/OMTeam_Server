package com.omteam.omt.security.auth.oauth.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.omteam.omt.security.auth.oauth.OAuthUserInfo;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GoogleUserInfo implements OAuthUserInfo {

    private final Payload payload;

    @Override
    public String getProviderUserId() {
        return payload.getSubject();
    }

    @Override
    public String getEmail() {
        return payload.getEmail();
    }
}
