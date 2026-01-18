package com.omteam.omt.security.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private long expiresIn;
    @Setter
    private boolean onboardingCompleted;

    public LoginResponse(String accessToken, String refreshToken, long expiresIn) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
    }
}
