package com.omteam.omt.security.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class RefreshTokenRequest {

    @NotBlank(message = "refreshToken은 필수입니다")
    private String refreshToken;
}
