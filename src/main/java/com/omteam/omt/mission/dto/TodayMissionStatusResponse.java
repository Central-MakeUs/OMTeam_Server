package com.omteam.omt.mission.dto;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TodayMissionStatusResponse {
    private LocalDate date;
    private boolean hasRecommendations;
    private boolean hasInProgressMission;
    private boolean hasCompletedMission;
    private RecommendedMissionResponse currentMission;
    private MissionResultResponse missionResult;
}
