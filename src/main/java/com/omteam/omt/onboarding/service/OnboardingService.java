package com.omteam.omt.onboarding.service;

import com.omteam.omt.common.exception.BusinessException;
import com.omteam.omt.common.exception.ErrorCode;
import com.omteam.omt.onboarding.dto.OnboardingRequest;
import com.omteam.omt.onboarding.dto.OnboardingResponse;
import com.omteam.omt.user.domain.User;
import com.omteam.omt.user.domain.UserNotificationSetting;
import com.omteam.omt.user.domain.UserOnboarding;
import com.omteam.omt.user.repository.UserNotificationSettingRepository;
import com.omteam.omt.user.repository.UserOnboardingRepository;
import com.omteam.omt.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final UserRepository userRepository;
    private final UserOnboardingRepository userOnboardingRepository;
    private final UserNotificationSettingRepository userNotificationSettingRepository;

    @Transactional
    public OnboardingResponse createOnboarding(Long userId, OnboardingRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!user.isOnboardingCompleted()) {
            throw new BusinessException(ErrorCode.ONBOARDING_NOT_COMPLETED);
        }

        UserOnboarding onboarding = userOnboardingRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ONBOARDING_NOT_FOUND));

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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!user.isOnboardingCompleted()) {
            throw new BusinessException(ErrorCode.ONBOARDING_NOT_COMPLETED);
        }

        UserOnboarding onboarding = userOnboardingRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ONBOARDING_NOT_FOUND));

        UserNotificationSetting notificationSetting = userNotificationSettingRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ONBOARDING_NOT_FOUND));

        return OnboardingResponse.of(user, onboarding, notificationSetting);
    }
}
