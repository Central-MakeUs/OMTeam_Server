package com.omteam.omt.onboarding.controller;

import com.omteam.omt.common.response.ApiResponse;
import com.omteam.omt.onboarding.dto.OnboardingRequest;
import com.omteam.omt.onboarding.dto.OnboardingResponse;
import com.omteam.omt.onboarding.dto.request.UpdateAppGoalRequest;
import com.omteam.omt.onboarding.dto.request.UpdateAvailableTimeRequest;
import com.omteam.omt.onboarding.dto.request.UpdateLifestyleRequest;
import com.omteam.omt.onboarding.dto.request.UpdateMinExerciseMinutesRequest;
import com.omteam.omt.onboarding.dto.request.UpdateNicknameRequest;
import com.omteam.omt.onboarding.dto.request.UpdateNotificationSettingRequest;
import com.omteam.omt.onboarding.dto.request.UpdatePreferredExerciseRequest;
import com.omteam.omt.onboarding.dto.request.UpdateSingleNotificationRequest;
import com.omteam.omt.onboarding.dto.request.UpdateSleepScheduleRequest;
import com.omteam.omt.onboarding.dto.request.UpdateWorkTimeRequest;
import com.omteam.omt.onboarding.service.OnboardingService;
import com.omteam.omt.security.principal.UserPrincipal;
import com.omteam.omt.user.domain.NotificationType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "온보딩", description = "온보딩 정보 등록/수정/조회 API")
@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    @Operation(
            summary = "온보딩 정보 등록",
            description = "사용자의 온보딩 정보를 등록합니다. 최초 1회만 등록 가능합니다."
    )
    @PostMapping
    public ApiResponse<OnboardingResponse> createOnboarding(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody OnboardingRequest request
    ) {
        return ApiResponse.success(
                onboardingService.createOnboarding(userPrincipal.userId(), request)
        );
    }

    @Operation(
            summary = "온보딩 정보 수정",
            description = "사용자의 온보딩 정보를 수정합니다."
    )
    @PutMapping
    public ApiResponse<OnboardingResponse> updateOnboarding(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody OnboardingRequest request
    ) {
        return ApiResponse.success(
                onboardingService.updateOnboarding(userPrincipal.userId(), request)
        );
    }

    @Operation(
            summary = "온보딩 정보 조회",
            description = "사용자의 온보딩 정보를 조회합니다."
    )
    @GetMapping
    public ApiResponse<OnboardingResponse> getOnboarding(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ApiResponse.success(
                onboardingService.getOnboarding(userPrincipal.userId())
        );
    }

    @Operation(
            summary = "닉네임 수정",
            description = "사용자의 닉네임을 수정합니다."
    )
    @PatchMapping("/nickname")
    public ApiResponse<OnboardingResponse> updateNickname(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody UpdateNicknameRequest request
    ) {
        return ApiResponse.success(
                onboardingService.updateNickname(userPrincipal.userId(), request.getNickname())
        );
    }

    @Operation(
            summary = "앱 사용 목적 수정",
            description = "사용자의 앱 사용 목적을 수정합니다."
    )
    @PatchMapping("/app-goal")
    public ApiResponse<OnboardingResponse> updateAppGoal(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody UpdateAppGoalRequest request
    ) {
        return ApiResponse.success(
                onboardingService.updateAppGoal(userPrincipal.userId(), request.getAppGoalText())
        );
    }

    @Operation(
            summary = "근무 시간 유형 수정",
            description = "사용자의 근무 시간 유형을 수정합니다."
    )
    @PatchMapping("/work-time")
    public ApiResponse<OnboardingResponse> updateWorkTime(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody UpdateWorkTimeRequest request
    ) {
        return ApiResponse.success(
                onboardingService.updateWorkTimeType(userPrincipal.userId(), request.getWorkTimeType())
        );
    }

    @Operation(
            summary = "운동 가능 시간대 수정",
            description = "사용자의 운동 가능 시간대를 수정합니다."
    )
    @PatchMapping("/available-time")
    public ApiResponse<OnboardingResponse> updateAvailableTime(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody UpdateAvailableTimeRequest request
    ) {
        return ApiResponse.success(
                onboardingService.updateAvailableTime(
                        userPrincipal.userId(),
                        request.getAvailableStartTime(),
                        request.getAvailableEndTime()
                )
        );
    }

    @Operation(
            summary = "최소 운동 시간 수정",
            description = "사용자의 최소 운동 시간을 수정합니다."
    )
    @PatchMapping("/min-exercise-minutes")
    public ApiResponse<OnboardingResponse> updateMinExerciseMinutes(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody UpdateMinExerciseMinutesRequest request
    ) {
        return ApiResponse.success(
                onboardingService.updateMinExerciseMinutes(userPrincipal.userId(), request.getMinExerciseMinutes())
        );
    }

    @Operation(
            summary = "선호 운동 수정",
            description = "사용자의 선호 운동을 수정합니다."
    )
    @PatchMapping("/preferred-exercise")
    public ApiResponse<OnboardingResponse> updatePreferredExercise(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody UpdatePreferredExerciseRequest request
    ) {
        return ApiResponse.success(
                onboardingService.updatePreferredExercise(userPrincipal.userId(), request.getPreferredExercises())
        );
    }

    @Operation(
            summary = "생활 패턴 수정",
            description = "사용자의 생활 패턴을 수정합니다."
    )
    @PatchMapping("/lifestyle")
    public ApiResponse<OnboardingResponse> updateLifestyle(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody UpdateLifestyleRequest request
    ) {
        return ApiResponse.success(
                onboardingService.updateLifestyleType(userPrincipal.userId(), request.getLifestyleType())
        );
    }

    @Operation(
            summary = "알림 설정 전체 수정",
            description = "사용자의 모든 알림 설정을 수정합니다."
    )
    @PatchMapping("/notification")
    public ApiResponse<OnboardingResponse> updateNotificationSetting(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody UpdateNotificationSettingRequest request
    ) {
        return ApiResponse.success(
                onboardingService.updateNotificationSetting(
                        userPrincipal.userId(),
                        request.getRemindEnabled(),
                        request.getCheckinEnabled(),
                        request.getReviewEnabled()
                )
        );
    }

    @Operation(
            summary = "기상/취침 시간 수정",
            description = "사용자의 기상 시간과 취침 시간을 수정합니다. (30분 단위, 선택)"
    )
    @PatchMapping("/sleep-schedule")
    public ApiResponse<OnboardingResponse> updateSleepSchedule(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody UpdateSleepScheduleRequest request
    ) {
        return ApiResponse.success(
                onboardingService.updateSleepSchedule(
                        userPrincipal.userId(),
                        request.getWakeUpTime(),
                        request.getBedTime()
                )
        );
    }

    @Operation(
            summary = "개별 알림 설정 수정",
            description = "특정 알림 타입의 설정을 수정합니다. (REMIND, CHECKIN, REVIEW)"
    )
    @PatchMapping("/notification/{type}")
    public ApiResponse<OnboardingResponse> updateSingleNotification(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable NotificationType type,
            @Valid @RequestBody UpdateSingleNotificationRequest request
    ) {
        return ApiResponse.success(
                onboardingService.updateSingleNotification(userPrincipal.userId(), type, request.getEnabled())
        );
    }
}
