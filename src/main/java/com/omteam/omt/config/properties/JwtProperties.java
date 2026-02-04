package com.omteam.omt.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    @NotBlank(message = "JWT secret은 필수입니다")
    @Size(min = 32, message = "JWT secret은 최소 32자 이상이어야 합니다")
    private String secret;

    @Positive(message = "access-token-expire-seconds는 양수여야 합니다")
    private long accessTokenExpireSeconds = 300;  // 기본 5분

    @Positive(message = "refresh-token-expire-seconds는 양수여야 합니다")
    private long refreshTokenExpireSeconds = 1209600;  // 기본 14일

    private String issuer = "omt-server";

    private String audience = "omt-client";
}
