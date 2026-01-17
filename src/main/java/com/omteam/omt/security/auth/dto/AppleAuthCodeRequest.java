package com.omteam.omt.security.auth.dto;

import lombok.Getter;

@Getter
public class AppleAuthCodeRequest {
    private String authorizationCode;
    private String idToken;
}
