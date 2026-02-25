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
import java.util.List;
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
                .preferredExercises(request.getPreferredExercises())
                .lifestyleType(request.getLifestyleType())
                .wakeUpTime(request.getWakeUpTime())
                .bedTime(request.getBedTime())
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
                request.getPreferredExercises(),
                request.getLifestyleType()
        );
        onboarding.updateSleepSchedule(request.getWakeUpTime(), request.getBedTime());

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
        OnboardingContext ctx = fetchOnboardingContext(userId);
        ctx.user.updateNickname(nickname);
        return OnboardingResponse.of(ctx.user, ctx.onboarding, ctx.notificationSetting);
    }

    @Transactional
    public OnboardingResponse updateAppGoal(Long userId, String appGoalText) {
        OnboardingContext ctx = fetchOnboardingContext(userId);
        ctx.onboarding.updateAppGoal(appGoalText);
        return OnboardingResponse.of(ctx.user, ctx.onboarding, ctx.notificationSetting);
    }

    @Transactional
    public OnboardingResponse updateWorkTimeType(Long userId, WorkTimeType workTimeType) {
        OnboardingContext ctx = fetchOnboardingContext(userId);
        ctx.onboarding.updateWorkTimeType(workTimeType);
        return OnboardingResponse.of(ctx.user, ctx.onboarding, ctx.notificationSetting);
    }

    @Transactional
    public OnboardingResponse updateAvailableTime(Long userId, LocalTime availableStartTime, LocalTime availableEndTime) {
        OnboardingContext ctx = fetchOnboardingContext(userId);
        ctx.onboarding.updateAvailableTime(availableStartTime, availableEndTime);
        return OnboardingResponse.of(ctx.user, ctx.onboarding, ctx.notificationSetting);
    }

    @Transactional
    public OnboardingResponse updateMinExerciseMinutes(Long userId, int minExerciseMinutes) {
        OnboardingContext ctx = fetchOnboardingContext(userId);
        ctx.onboarding.updateMinExerciseMinutes(minExerciseMinutes);
        return OnboardingResponse.of(ctx.user, ctx.onboarding, ctx.notificationSetting);
    }

    @Transactional
    public OnboardingResponse updatePreferredExercise(Long userId, List<String> preferredExercises) {
        OnboardingContext ctx = fetchOnboardingContext(userId);
        ctx.onboarding.updatePreferredExercise(preferredExercises);
        return OnboardingResponse.of(ctx.user, ctx.onboarding, ctx.notificationSetting);
    }

    @Transactional
    public OnboardingResponse updateLifestyleType(Long userId, LifestyleType lifestyleType) {
        OnboardingContext ctx = fetchOnboardingContext(userId);
        ctx.onboarding.updateLifestyleType(lifestyleType);
        return OnboardingResponse.of(ctx.user, ctx.onboarding, ctx.notificationSetting);
    }

    @Transactional
    public OnboardingResponse updateNotificationSetting(Long userId, boolean remindEnabled, boolean checkinEnabled, boolean reviewEnabled) {
        OnboardingContext ctx = fetchOnboardingContext(userId);
        ctx.notificationSetting.update(remindEnabled, checkinEnabled, reviewEnabled);
        return OnboardingResponse.of(ctx.user, ctx.onboarding, ctx.notificationSetting);
    }

    @Transactional
    public OnboardingResponse updateSleepSchedule(Long userId, LocalTime wakeUpTime, LocalTime bedTime) {
        OnboardingContext ctx = fetchOnboardingContext(userId);
        ctx.onboarding.updateSleepSchedule(wakeUpTime, bedTime);
        return OnboardingResponse.of(ctx.user, ctx.onboarding, ctx.notificationSetting);
    }

    @Transactional
    public OnboardingResponse updateSingleNotification(Long userId, NotificationType type, boolean enabled) {
        OnboardingContext ctx = fetchOnboardingContext(userId);
        ctx.notificationSetting.updateNotification(type, enabled);
        return OnboardingResponse.of(ctx.user, ctx.onboarding, ctx.notificationSetting);
    }

    private OnboardingContext fetchOnboardingContext(Long userId) {
        User user = userQueryService.getUser(userId);
        if (!user.isOnboardingCompleted()) {
            throw new BusinessException(ErrorCode.ONBOARDING_NOT_COMPLETED);
        }
        UserOnboarding onboarding = userQueryService.getUserOnboarding(userId);
        UserNotificationSetting notificationSetting = userNotificationSettingRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ONBOARDING_NOT_FOUND));
        return new OnboardingContext(user, onboarding, notificationSetting);
    }

    private record OnboardingContext(User user, UserOnboarding onboarding, UserNotificationSetting notificationSetting) {}
}
