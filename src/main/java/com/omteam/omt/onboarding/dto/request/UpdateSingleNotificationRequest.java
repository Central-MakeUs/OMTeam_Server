package com.omteam.omt.onboarding.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "개별 알림 설정 수정 요청")
public class UpdateSingleNotificationRequest {

    @Schema(description = "알림 활성화 여부", example = "true")
    @NotNull(message = "알림 활성화 여부는 필수입니다")
    private Boolean enabled;
}
