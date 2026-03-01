package com.omteam.omt.mission.dto;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DailyMissionRecommendResponse {
    private LocalDate missionDate;
    private List<RecommendedMissionResponse> recommendations;
    private boolean hasInProgressMission;
    private RecommendedMissionResponse inProgressMission;
    private boolean isFallback;
}
