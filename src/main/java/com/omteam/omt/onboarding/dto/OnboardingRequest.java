package com.omteam.omt.onboarding.dto;

import com.omteam.omt.user.domain.LifestyleType;
import com.omteam.omt.user.domain.WorkTimeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalTime;
import lombok.Getter;

@Getter
public class OnboardingRequest {

    @NotBlank(message = "닉네임은 필수입니다")
    private String nickname;

    @NotBlank(message = "앱 사용 목적은 필수입니다")
    private String appGoalText;

    @NotNull(message = "근무 시간 유형은 필수입니다")
    private WorkTimeType workTimeType;

    @NotNull(message = "운동 가능 시작 시간은 필수입니다")
    private LocalTime availableStartTime;

    @NotNull(message = "운동 가능 종료 시간은 필수입니다")
    private LocalTime availableEndTime;

    @Positive(message = "최소 운동 시간은 양수여야 합니다")
    private int minExerciseMinutes;

    @NotBlank(message = "선호 운동은 필수입니다")
    private String preferredExerciseText;

    @NotNull(message = "생활 패턴은 필수입니다")
    private LifestyleType lifestyleType;

    @NotNull(message = "리마인드 알림 설정은 필수입니다")
    private Boolean remindEnabled;

    @NotNull(message = "체크인 알림 설정은 필수입니다")
    private Boolean checkinEnabled;

    @NotNull(message = "리뷰 알림 설정은 필수입니다")
    private Boolean reviewEnabled;
}
