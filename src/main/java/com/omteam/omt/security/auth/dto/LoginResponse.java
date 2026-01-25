package com.omteam.omt.security.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {

    private final String accessToken;
    private final String refreshToken;
    private final long expiresIn;
    private final boolean onboardingCompleted;
}
