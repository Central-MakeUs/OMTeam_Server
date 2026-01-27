package com.omteam.omt.mission.client.dto;

import com.omteam.omt.mission.domain.Mission;
import com.omteam.omt.mission.domain.MissionType;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AiMissionRecommendResponse {

    private List<RecommendedMission> missions;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class RecommendedMission {
        private String name;
        private MissionType type;
        private int difficulty;
        private int estimatedMinutes;
        private int estimatedCalories;

        public Mission toEntity() {
            return Mission.builder()
                    .name(name)
                    .type(type)
                    .difficulty(difficulty)
                    .estimatedMinutes(estimatedMinutes)
                    .estimatedCalories(estimatedCalories)
                    .build();
        }
    }
}
