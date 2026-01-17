package com.omteam.omt.security.auth.oauth;

public interface OAuthUserInfo {
    String getProviderUserId();

    String getEmail();
}
