package com.omteam.omt.mission.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MissionSelectRequest {

    @NotNull(message = "추천 미션 ID는 필수입니다")
    private Long recommendedMissionId;
}
