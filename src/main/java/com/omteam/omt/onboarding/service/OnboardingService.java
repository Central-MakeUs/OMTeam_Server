package com.omteam.omt.onboarding.service;

import com.omteam.omt.common.exception.BusinessException;
import com.omteam.omt.common.exception.ErrorCode;
import com.omteam.omt.onboarding.dto.OnboardingRequest;
import com.omteam.omt.onboarding.dto.OnboardingResponse;
import com.omteam.omt.user.domain.LifestyleType;
import com.omteam.omt.user.domain.NotificationType;
import com.omteam.omt.user.domain.User;
import com.omteam.omt.user.domain.UserNotificationSetting;
import com.omteam.omt.user.domain.UserOnboarding;
import com.omteam.omt.user.domain.WorkTimeType;
import com.omteam.omt.user.repository.UserNotificationSettingRepository;
import com.omteam.omt.user.repository.UserOnboardingRepository;
import com.omteam.omt.user.service.UserQueryService;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final UserQueryService userQueryService;
    private final UserOnboardingRepository userOnboardingRepository;
    private final UserNotificationSettingRepository userNotificationSettingRepository;

    @Transactional
    public OnboardingResponse createOnboarding(Long userId, OnboardingRequest request) {
        User user = userQueryService.getUser(userId);

        if (user.isOnboardingCompleted()) {
            throw new BusinessException(ErrorCode.ONBOARDING_ALREADY_COMPLETED);
        }

        user.completeOnboarding(request.getNickname());

        UserOnboarding onboarding = UserOnboarding.builder()
                .user(user)
                .appGoalText(request.getAppGoalText())
                .workTimeType(request.getWorkTimeType())
                .availableStartTime(request.getAvailableStartTime())
                .availableEndTime(request.getAvailableEndTime())
                .minExerciseMinutes(request.getMinExerciseMinutes())
                .preferredExerciseText(request.getPreferredExerciseText())
                .lifestyleType(request.getLifestyleType())
                .build();

        UserNotificationSetting notificationSetting = UserNotificationSetting.builder()
                .user(user)
                .remindEnabled(request.getRemindEnabled())
                .checkinEnabled(request.getCheckinEnabled())
                .reviewEnabled(request.getReviewEnabled())
                .build();

        userOnboardingRepository.save(onboarding);
        userNotificationSettingRepository.save(notificationSetting);

        return OnboardingResponse.of(user, onboarding, notificationSetting);
    }

    @Transactional
    public OnboardingResponse updateOnboarding(Long userId, OnboardingRequest request) {
        User user = userQueryService.getUser(userId);

        if (!user.isOnboardingCompleted()) {
            throw new BusinessException(ErrorCode.ONBOARDING_NOT_COMPLETED);
        }

        UserOnboarding onboarding = userQueryService.getUserOnboarding(userId);

        UserNotificationSetting notificationSetting = userNotificationSettingRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ONBOARDING_NOT_FOUND));

        user.updateNickname(request.getNickname());

        onboarding.update(
                request.getAppGoalText(),
                request.getWorkTimeType(),
                request.getAvailableStartTime(),
                request.getAvailableEndTime(),
                request.getMinExerciseMinutes(),
                request.getPreferredExerciseText(),
                request.getLifestyleType()
        );

        notificationSetting.update(
                request.getRemindEnabled(),
                request.getCheckinEnabled(),
                request.getReviewEnabled()
        );

        return OnboardingResponse.of(user, onboarding, notificationSetting);
    }

    @Transactional(readOnly = true)
    public OnboardingResponse getOnboarding(Long userId) {
        User user = userQueryService.getUser(userId);

        if (!user.isOnboardingCompleted()) {
            throw new BusinessException(ErrorCode.ONBOARDING_NOT_COMPLETED);
        }

        UserOnboarding onboarding = userQueryService.getUserOnboarding(userId);

        UserNotificationSetting notificationSetting = userNotificationSettingRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ONBOARDING_NOT_FOUND));

        return OnboardingResponse.of(user, onboarding, notificationSetting);
    }

    @Transactional
    public OnboardingResponse updateNickname(Long userId, String nickname) {
        User user = validateOnboardingCompleted(userId);
        user.updateNickname(nickname);
        return buildOnboardingResponse(userId, user);
    }

    @Transactional
    public OnboardingResponse updateAppGoal(Long userId, String appGoalText) {
        User user = validateOnboardingCompleted(userId);
        UserOnboarding onboarding = userQueryService.getUserOnboarding(userId);
        onboarding.updateAppGoal(appGoalText);
        return buildOnboardingResponse(userId, user);
    }

    @Transactional
    public OnboardingResponse updateWorkTimeType(Long userId, WorkTimeType workTimeType) {
        User user = validateOnboardingCompleted(userId);
        UserOnboarding onboarding = userQueryService.getUserOnboarding(userId);
        onboarding.updateWorkTimeType(workTimeType);
        return buildOnboardingResponse(userId, user);
    }

    @Transactional
    public OnboardingResponse updateAvailableTime(Long userId, LocalTime availableStartTime, LocalTime availableEndTime) {
        User user = validateOnboardingCompleted(userId);
        UserOnboarding onboarding = userQueryService.getUserOnboarding(userId);
        onboarding.updateAvailableTime(availableStartTime, availableEndTime);
        return buildOnboardingResponse(userId, user);
    }

    @Transactional
    public OnboardingResponse updateMinExerciseMinutes(Long userId, int minExerciseMinutes) {
        User user = validateOnboardingCompleted(userId);
        UserOnboarding onboarding = userQueryService.getUserOnboarding(userId);
        onboarding.updateMinExerciseMinutes(minExerciseMinutes);
        return buildOnboardingResponse(userId, user);
    }

    @Transactional
    public OnboardingResponse updatePreferredExercise(Long userId, String preferredExerciseText) {
        User user = validateOnboardingCompleted(userId);
        UserOnboarding onboarding = userQueryService.getUserOnboarding(userId);
        onboarding.updatePreferredExercise(preferredExerciseText);
        return buildOnboardingResponse(userId, user);
    }

    @Transactional
    public OnboardingResponse updateLifestyleType(Long userId, LifestyleType lifestyleType) {
        User user = validateOnboardingCompleted(userId);
        UserOnboarding onboarding = userQueryService.getUserOnboarding(userId);
        onboarding.updateLifestyleType(lifestyleType);
        return buildOnboardingResponse(userId, user);
    }

    @Transactional
    public OnboardingResponse updateNotificationSetting(Long userId, boolean remindEnabled, boolean checkinEnabled, boolean reviewEnabled) {
        User user = validateOnboardingCompleted(userId);
        UserNotificationSetting notificationSetting = userNotificationSettingRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ONBOARDING_NOT_FOUND));
        notificationSetting.update(remindEnabled, checkinEnabled, reviewEnabled);
        return buildOnboardingResponse(userId, user);
    }

    @Transactional
    public OnboardingResponse updateSingleNotification(Long userId, NotificationType type, boolean enabled) {
        User user = validateOnboardingCompleted(userId);
        UserNotificationSetting notificationSetting = userNotificationSettingRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ONBOARDING_NOT_FOUND));
        notificationSetting.updateNotification(type, enabled);
        return buildOnboardingResponse(userId, user);
    }

    private User validateOnboardingCompleted(Long userId) {
        User user = userQueryService.getUser(userId);
        if (!user.isOnboardingCompleted()) {
            throw new BusinessException(ErrorCode.ONBOARDING_NOT_COMPLETED);
        }
        return user;
    }

    private OnboardingResponse buildOnboardingResponse(Long userId, User user) {
        UserOnboarding onboarding = userQueryService.getUserOnboarding(userId);
        UserNotificationSetting notificationSetting = userNotificationSettingRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ONBOARDING_NOT_FOUND));
        return OnboardingResponse.of(user, onboarding, notificationSetting);
    }
}
