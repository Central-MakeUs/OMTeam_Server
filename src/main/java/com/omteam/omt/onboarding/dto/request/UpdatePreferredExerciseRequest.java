package com.omteam.omt.onboarding.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "선호 운동 수정 요청")
public class UpdatePreferredExerciseRequest {

    @Schema(description = "선호 운동", example = "러닝, 홈트레이닝")
    @NotBlank(message = "선호 운동은 필수입니다")
    private String preferredExerciseText;
}
