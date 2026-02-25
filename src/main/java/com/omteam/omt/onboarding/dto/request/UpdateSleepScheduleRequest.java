package com.omteam.omt.onboarding.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "기상/취침 시간 수정 요청")
public class UpdateSleepScheduleRequest {

    @Schema(description = "기상 시간 (HH:mm, 30분 단위, 선택)", example = "07:00")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime wakeUpTime;

    @Schema(description = "취침 시간 (HH:mm, 30분 단위, 선택)", example = "23:00")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime bedTime;

    @AssertTrue(message = "기상 시간은 30분 단위여야 합니다 (예: 07:00, 07:30)")
    public boolean isWakeUpTimeValid() {
        return wakeUpTime == null || wakeUpTime.getMinute() % 30 == 0;
    }

    @AssertTrue(message = "취침 시간은 30분 단위여야 합니다 (예: 23:00, 23:30)")
    public boolean isBedTimeValid() {
        return bedTime == null || bedTime.getMinute() % 30 == 0;
    }
}
