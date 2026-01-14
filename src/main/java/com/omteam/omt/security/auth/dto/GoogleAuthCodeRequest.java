package com.omteam.omt.security.auth.dto;

import lombok.Getter;

@Getter
public class GoogleAuthCodeRequest {
    private String authorizationCode;
}
