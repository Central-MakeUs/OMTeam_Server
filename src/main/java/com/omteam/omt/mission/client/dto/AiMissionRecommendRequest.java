package com.omteam.omt.mission.client.dto;

import com.omteam.omt.common.ai.dto.UserContext;
import com.omteam.omt.mission.domain.MissionResult;
import com.omteam.omt.mission.domain.MissionType;
import com.omteam.omt.user.domain.LifestyleType;
import com.omteam.omt.user.domain.WorkTimeType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiMissionRecommendRequest {

    private Long userId;
    private UserContext userContext;
    private OnboardingData onboarding;
    private List<MissionHistory> recentMissionHistory;
    private List<String> weeklyFailureReasons;

    @Getter
    @Builder
    public static class OnboardingData {
        private String appGoal;
        private WorkTimeType workTimeType;
        private String availableStartTime;
        private String availableEndTime;
        private int minExerciseMinutes;
        private List<String> preferredExercises;
        private LifestyleType lifestyleType;

        public static OnboardingData from(
                String appGoalText,
                WorkTimeType workTimeType,
                LocalTime availableStartTime,
                LocalTime availableEndTime,
                int minExerciseMinutes,
                List<String> preferredExercises,
                LifestyleType lifestyleType
        ) {
            return OnboardingData.builder()
                    .appGoal(appGoalText)
                    .workTimeType(workTimeType)
                    .availableStartTime(availableStartTime.toString())
                    .availableEndTime(availableEndTime.toString())
                    .minExerciseMinutes(minExerciseMinutes)
                    .preferredExercises(preferredExercises)
                    .lifestyleType(lifestyleType)
                    .build();
        }
    }

    @Getter
    @Builder
    public static class MissionHistory {
        private String date;
        private MissionType missionType;
        private int difficulty;
        private MissionResult result;
        private String failureReason;

        public static MissionHistory of(
                LocalDate date,
                MissionType missionType,
                int difficulty,
                MissionResult result,
                String failureReason
        ) {
            return MissionHistory.builder()
                    .date(date.toString())
                    .missionType(missionType)
                    .difficulty(difficulty)
                    .result(result)
                    .failureReason(failureReason)
                    .build();
        }
    }
}
