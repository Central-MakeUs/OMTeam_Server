package com.omteam.omt.security.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class OAuthLoginRequest {

    @NotBlank(message = "idToken은 필수입니다")
    private String idToken;
}
