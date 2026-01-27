package com.omteam.omt.mission.dto;

import com.omteam.omt.mission.domain.Mission;
import com.omteam.omt.mission.domain.MissionType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MissionResponse {

    private Long id;
    private String name;

    @Schema(
            description = """
                    미션 타입
                    - EXERCISE: 운동
                    - DIET: 식단
                    """,
            allowableValues = {"EXERCISE", "DIET"}
    )
    private MissionType type;
    
    @Schema(description = "미션 난이도 (1~5, 별 개수)")
    private int difficulty;

    @Schema(description = "예상 소요 시간")
    private int estimatedMinutes;
    
    @Schema(description = "예상 소모 칼로리")
    private int estimatedCalories;

    public static MissionResponse from(Mission mission) {
        return MissionResponse.builder()
                .id(mission.getId())
                .name(mission.getName())
                .type(mission.getType())
                .difficulty(mission.getDifficulty())
                .estimatedMinutes(mission.getEstimatedMinutes())
                .estimatedCalories(mission.getEstimatedCalories())
                .build();
    }
}
