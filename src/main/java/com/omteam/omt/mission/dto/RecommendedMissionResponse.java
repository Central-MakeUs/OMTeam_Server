package com.omteam.omt.mission.dto;

import com.omteam.omt.mission.domain.DailyRecommendedMission;
import com.omteam.omt.mission.domain.RecommendedMissionStatus;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecommendedMissionResponse {

    private Long recommendedMissionId;
    private LocalDate missionDate;

    @Schema(
            description = "추천 미션의 현재 상태",
            allowableValues = {
                "RECOMMENDED",
                "SELECTED",
                "IN_PROGRESS",
                "COMPLETED",
                "EXPIRED"
            },
            example = "RECOMMENDED"
    )
    private RecommendedMissionStatus status;
    private MissionResponse mission;

    public static RecommendedMissionResponse from(DailyRecommendedMission drm) {
        return RecommendedMissionResponse.builder()
                .recommendedMissionId(drm.getId())
                .missionDate(drm.getMissionDate())
                .status(drm.getStatus())
                .mission(MissionResponse.from(drm.getMission()))
                .build();
    }

    public static List<RecommendedMissionResponse> fromList(List<DailyRecommendedMission> missions) {
        return missions.stream()
                .map(RecommendedMissionResponse::from)
                .toList();
    }
}
