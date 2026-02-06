package com.omteam.omt.onboarding.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "운동 가능 시간대 수정 요청")
public class UpdateAvailableTimeRequest {

    @Schema(
            description = "운동 가능 시작 시간 (HH:mm)",
            type = "string",
            format = "time",
            example = "18:30"
    )
    @NotNull(message = "운동 가능 시작 시간은 필수입니다")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime availableStartTime;

    @Schema(
            description = "운동 가능 종료 시간 (HH:mm)",
            type = "string",
            format = "time",
            example = "21:00"
    )
    @NotNull(message = "운동 가능 종료 시간은 필수입니다")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime availableEndTime;
}
