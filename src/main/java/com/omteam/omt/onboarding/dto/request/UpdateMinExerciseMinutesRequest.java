package com.omteam.omt.onboarding.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "최소 운동 시간 수정 요청")
public class UpdateMinExerciseMinutesRequest {

    @Schema(description = "최소 운동 시간 (분)", example = "30")
    @Positive(message = "최소 운동 시간은 양수여야 합니다")
    private int minExerciseMinutes;
}
