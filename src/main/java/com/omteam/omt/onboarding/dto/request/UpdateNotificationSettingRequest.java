package com.omteam.omt.onboarding.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "알림 설정 전체 수정 요청")
public class UpdateNotificationSettingRequest {

    @Schema(description = "리마인드 알림 수신 여부", example = "true")
    @NotNull(message = "리마인드 알림 설정은 필수입니다")
    private Boolean remindEnabled;

    @Schema(description = "체크인 알림 수신 여부", example = "true")
    @NotNull(message = "체크인 알림 설정은 필수입니다")
    private Boolean checkinEnabled;

    @Schema(description = "리뷰 알림 수신 여부", example = "true")
    @NotNull(message = "리뷰 알림 설정은 필수입니다")
    private Boolean reviewEnabled;
}
