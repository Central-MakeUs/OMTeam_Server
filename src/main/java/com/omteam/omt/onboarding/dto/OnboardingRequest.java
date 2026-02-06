package com.omteam.omt.onboarding.dto;

import com.omteam.omt.user.domain.LifestyleType;
import com.omteam.omt.user.domain.WorkTimeType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import jakarta.validation.constraints.Size;
import java.time.LocalTime;
import java.util.List;

import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import lombok.Getter;

@Getter
@Setter
@NoArgsConstructor
public class OnboardingRequest {

    @NotBlank(message = "닉네임은 필수입니다")
    @Size(max = 8, message = "닉네임은 최대 8글자까지 가능합니다")
    private String nickname;

    @NotBlank(message = "앱 사용 목적은 필수입니다")
    private String appGoalText;

    @Schema(
            description = """
                    근무 시간 유형
                    - FIXED: 고정 근무
                    - SHIFT: 스케줄 근무 (교대 근무)
                    """,
            allowableValues = {"FIXED", "SHIFT"}
    )
    @NotNull(message = "근무 시간 유형은 필수입니다")
    private WorkTimeType workTimeType;

    @Schema(
            description = "운동 가능 시작 시간 (HH:mm)",
            type = "string",
            format = "time",
            example = "18:30"
    )
    @NotNull(message = "운동 가능 시작 시간은 필수입니다")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime availableStartTime;

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime availableEndTime;

    @Positive(message = "최소 운동 시간은 양수여야 합니다")
    private int minExerciseMinutes;

    @Schema(description = "선호 운동", example = "[\"스트레칭\", \"요가\"]")
    @NotEmpty(message = "선호 운동은 필수입니다")
    private List<String> preferredExercises;

    @Schema(
            description = """
                    최근 한 달 기준 생활 패턴
                    - REGULAR_DAYTIME: 비교적 규칙적인 평일 주간 근무
                    - IRREGULAR_OVERTIME: 야근/불규칙한 일정이 잦음
                    - SHIFT_NIGHT: 교대 또는 밤샘 근무
                    - VARIABLE_DAILY: 일정이 매일 달라 예측이 어려움
                    """,
            allowableValues = {
                    "REGULAR_DAYTIME",
                    "IRREGULAR_OVERTIME",
                    "SHIFT_NIGHT",
                    "VARIABLE_DAILY"
            }
    )
    @NotNull(message = "생활 패턴은 필수입니다")
    private LifestyleType lifestyleType;

    @Schema(
            description = "리마인드 알림 수신 여부 (true: 수신, false: 미수신)",
            example = "true"
    )
    @NotNull(message = "리마인드 알림 설정은 필수입니다")
    private Boolean remindEnabled;

    @Schema(
            description = "체크인 알림 수신 여부 (true: 수신, false: 미수신)",
            example = "true"
    )
    @NotNull(message = "체크인 알림 설정은 필수입니다")
    private Boolean checkinEnabled;

    @Schema(
            description = "리뷰(피드백) 알림 수신 여부 (true: 수신, false: 미수신)",
            example = "true"
    )
    @NotNull(message = "리뷰 알림 설정은 필수입니다")
    private Boolean reviewEnabled;
}
