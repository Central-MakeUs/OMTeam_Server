package com.omteam.omt.onboarding.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "앱 사용 목적 수정 요청")
public class UpdateAppGoalRequest {

    @Schema(description = "앱 사용 목적", example = "체중 감량")
    @NotBlank(message = "앱 사용 목적은 필수입니다")
    private String appGoalText;
}
