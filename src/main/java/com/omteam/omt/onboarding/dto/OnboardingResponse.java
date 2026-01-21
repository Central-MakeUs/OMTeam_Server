package com.omteam.omt.onboarding.dto;

import com.omteam.omt.user.domain.LifestyleType;
import com.omteam.omt.user.domain.User;
import com.omteam.omt.user.domain.UserNotificationSetting;
import com.omteam.omt.user.domain.UserOnboarding;
import com.omteam.omt.user.domain.WorkTimeType;
import java.time.LocalTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OnboardingResponse {

    private String nickname;
    private String appGoalText;
    private WorkTimeType workTimeType;
    private LocalTime availableStartTime;
    private LocalTime availableEndTime;
    private int minExerciseMinutes;
    private String preferredExerciseText;
    private LifestyleType lifestyleType;
    private boolean remindEnabled;
    private boolean checkinEnabled;
    private boolean reviewEnabled;

    public static OnboardingResponse of(
            User user,
            UserOnboarding onboarding,
            UserNotificationSetting notificationSetting
    ) {
        return OnboardingResponse.builder()
                .nickname(user.getNickname())
                .appGoalText(onboarding.getAppGoalText())
                .workTimeType(onboarding.getWorkTimeType())
                .availableStartTime(onboarding.getAvailableStartTime())
                .availableEndTime(onboarding.getAvailableEndTime())
                .minExerciseMinutes(onboarding.getMinExerciseMinutes())
                .preferredExerciseText(onboarding.getPreferredExerciseText())
                .lifestyleType(onboarding.getLifestyleType())
                .remindEnabled(notificationSetting.isRemindEnabled())
                .checkinEnabled(notificationSetting.isCheckinEnabled())
                .reviewEnabled(notificationSetting.isReviewEnabled())
                .build();
    }
}
