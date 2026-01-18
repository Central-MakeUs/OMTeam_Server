package com.omteam.omt.security.auth.oauth;

import com.omteam.omt.user.domain.SocialProvider;

public interface OAuthClient {
    SocialProvider getProvider();
    OAuthUserInfo getUserInfo(String idToken);
}

