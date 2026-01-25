package com.omteam.omt.character.client.dto;

import com.omteam.omt.mission.domain.MissionDifficulty;
import com.omteam.omt.mission.domain.MissionResult;
import com.omteam.omt.mission.domain.MissionType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiEncouragementRequest {

    private Long userId;
    private String targetDate;
    private TodayMission todayMission;

    @Getter
    @Builder
    public static class TodayMission {
        private MissionType missionType;
        private MissionDifficulty difficulty;
        private MissionResult result;
        private String failureReason;
    }
}
