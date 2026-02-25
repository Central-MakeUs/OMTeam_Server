package com.omteam.omt.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "FCM 토큰 등록 요청")
public class FcmTokenRequest {

    @Schema(description = "FCM 디바이스 토큰", example = "eX4mPl3T0k3n...")
    @NotBlank(message = "FCM 토큰은 필수입니다")
    private String fcmToken;
}
