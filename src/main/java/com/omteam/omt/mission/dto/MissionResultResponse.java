package com.omteam.omt.mission.dto;

import com.omteam.omt.mission.domain.DailyMissionResult;
import com.omteam.omt.mission.domain.MissionResult;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MissionResultResponse {
    private Long id;
    private LocalDate missionDate;
    private MissionResult result;
    private String failureReason;
    private MissionResponse mission;

    public static MissionResultResponse from(DailyMissionResult dmr) {
        return MissionResultResponse.builder()
                .id(dmr.getId())
                .missionDate(dmr.getMissionDate())
                .result(dmr.getResult())
                .failureReason(dmr.getFailureReason())
                .mission(MissionResponse.from(dmr.getMission()))
                .build();
    }
}
