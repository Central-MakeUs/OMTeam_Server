package com.omteam.omt.report.client.dto;

import com.omteam.omt.mission.domain.MissionResult;
import com.omteam.omt.mission.domain.MissionType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiDailyAnalysisRequest {

    private Long userId;
    private String targetDate;
    private TodayMission todayMission;

    //recent summary 추가 필요

    @Getter
    @Builder
    public static class TodayMission {
        private MissionType missionType;
        private int difficulty;
        private MissionResult result;
        private String failureReason;
    }
}
