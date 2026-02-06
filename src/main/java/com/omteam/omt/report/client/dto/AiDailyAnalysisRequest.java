package com.omteam.omt.report.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.omteam.omt.common.ai.dto.UserContext;
import com.omteam.omt.mission.domain.MissionResult;
import com.omteam.omt.mission.domain.MissionType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiDailyAnalysisRequest {

    private Long userId;
    private String targetDate;
    private UserContext userContext;
    private TodayMission todayMission;

    //recent summary 추가 필요

    @Getter
    @Builder
    public static class TodayMission {
        private MissionType missionType;
        private int difficulty;
        private String status;
        private String failureReason;
    }
}
