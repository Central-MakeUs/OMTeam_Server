package com.omteam.omt.mission.dto;

import com.omteam.omt.mission.domain.Mission;
import com.omteam.omt.mission.domain.MissionDifficulty;
import com.omteam.omt.mission.domain.MissionType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MissionResponse {
    private Long id;
    private String name;
    private MissionType type;
    private MissionDifficulty difficulty;
    private int estimatedMinutes;
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
