package com.omteam.omt.onboarding.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.omteam.omt.common.exception.BusinessException;
import com.omteam.omt.common.exception.ErrorCode;
import com.omteam.omt.onboarding.dto.OnboardingRequest;
import com.omteam.omt.onboarding.dto.OnboardingResponse;
import com.omteam.omt.user.domain.LifestyleType;
import com.omteam.omt.user.domain.User;
import com.omteam.omt.user.domain.UserNotificationSetting;
import com.omteam.omt.user.domain.UserOnboarding;
import com.omteam.omt.user.domain.WorkTimeType;
import com.omteam.omt.user.repository.UserNotificationSettingRepository;
import com.omteam.omt.user.repository.UserOnboardingRepository;
import com.omteam.omt.user.service.UserQueryService;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("[단위] OnboardingService")
class OnboardingServiceTest {

    @Mock
    UserQueryService userQueryService;

    @Mock
    UserOnboardingRepository userOnboardingRepository;

    @Mock
    UserNotificationSettingRepository userNotificationSettingRepository;

    @InjectMocks
    OnboardingService onboardingService;

    final Long userId = 1L;

    @Nested
    @DisplayName("온보딩 등록 (createOnboarding)")
    class CreateOnboarding {

        @Test
        @DisplayName("성공 - wakeUpTime/bedTime 포함하여 온보딩 등록")
        void success_withSleepSchedule() {
            // given
            User user = createIncompleteUser();
            given(userQueryService.getUser(userId)).willReturn(user);

            OnboardingRequest request = createOnboardingRequest(
                    LocalTime.of(7, 0), LocalTime.of(23, 30)
            );

            // when
            OnboardingResponse response = onboardingService.createOnboarding(userId, request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getNickname()).isEqualTo("테스트유저");
            assertThat(response.getWakeUpTime()).isEqualTo(LocalTime.of(7, 0));
            assertThat(response.getBedTime()).isEqualTo(LocalTime.of(23, 30));

            then(userOnboardingRepository).should().save(any(UserOnboarding.class));
            then(userNotificationSettingRepository).should().save(any(UserNotificationSetting.class));
        }

        @Test
        @DisplayName("성공 - wakeUpTime/bedTime null로 온보딩 등록")
        void success_withoutSleepSchedule() {
            // given
            User user = createIncompleteUser();
            given(userQueryService.getUser(userId)).willReturn(user);

            OnboardingRequest request = createOnboardingRequest(null, null);

            // when
            OnboardingResponse response = onboardingService.createOnboarding(userId, request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getWakeUpTime()).isNull();
            assertThat(response.getBedTime()).isNull();

            then(userOnboardingRepository).should().save(any(UserOnboarding.class));
        }

        @Test
        @DisplayName("실패 - 온보딩이 이미 완료된 경우 ONBOARDING_ALREADY_COMPLETED 예외")
        void fail_onboardingAlreadyCompleted() {
            // given
            User user = createCompletedUser();
            given(userQueryService.getUser(userId)).willReturn(user);

            OnboardingRequest request = createOnboardingRequest(LocalTime.of(7, 0), LocalTime.of(23, 0));

            // when & then
            assertThatThrownBy(() -> onboardingService.createOnboarding(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ONBOARDING_ALREADY_COMPLETED);

            then(userOnboardingRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("온보딩 수정 (updateOnboarding)")
    class UpdateOnboarding {

        @Test
        @DisplayName("성공 - wakeUpTime/bedTime 포함하여 온보딩 수정")
        void success_withSleepSchedule() {
            // given
            User user = createCompletedUser();
            UserOnboarding onboarding = createUserOnboarding(user);
            UserNotificationSetting notificationSetting = createNotificationSetting(user);

            given(userQueryService.getUser(userId)).willReturn(user);
            given(userQueryService.getUserOnboarding(userId)).willReturn(onboarding);
            given(userNotificationSettingRepository.findByUserId(userId)).willReturn(Optional.of(notificationSetting));

            OnboardingRequest request = createOnboardingRequest(
                    LocalTime.of(6, 30), LocalTime.of(22, 0)
            );

            // when
            OnboardingResponse response = onboardingService.updateOnboarding(userId, request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getWakeUpTime()).isEqualTo(LocalTime.of(6, 30));
            assertThat(response.getBedTime()).isEqualTo(LocalTime.of(22, 0));
        }

        @Test
        @DisplayName("성공 - wakeUpTime/bedTime null로 수정 (기존 값 초기화)")
        void success_clearingSleepSchedule() {
            // given
            User user = createCompletedUser();
            UserOnboarding onboarding = createUserOnboardingWithSleepSchedule(
                    user, LocalTime.of(7, 0), LocalTime.of(23, 0)
            );
            UserNotificationSetting notificationSetting = createNotificationSetting(user);

            given(userQueryService.getUser(userId)).willReturn(user);
            given(userQueryService.getUserOnboarding(userId)).willReturn(onboarding);
            given(userNotificationSettingRepository.findByUserId(userId)).willReturn(Optional.of(notificationSetting));

            OnboardingRequest request = createOnboardingRequest(null, null);

            // when
            OnboardingResponse response = onboardingService.updateOnboarding(userId, request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getWakeUpTime()).isNull();
            assertThat(response.getBedTime()).isNull();
        }

        @Test
        @DisplayName("실패 - 온보딩 미완료 상태에서 수정 시 ONBOARDING_NOT_COMPLETED 예외")
        void fail_onboardingNotCompleted() {
            // given
            User user = createIncompleteUser();
            given(userQueryService.getUser(userId)).willReturn(user);

            OnboardingRequest request = createOnboardingRequest(LocalTime.of(7, 0), LocalTime.of(23, 0));

            // when & then
            assertThatThrownBy(() -> onboardingService.updateOnboarding(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ONBOARDING_NOT_COMPLETED);
        }
    }

    @Nested
    @DisplayName("기상/취침 시간 수정 (updateSleepSchedule)")
    class UpdateSleepSchedule {

        @Test
        @DisplayName("성공 - 기상/취침 시간이 수정된다")
        void success_withBothTimes() {
            // given
            User user = createCompletedUser();
            UserOnboarding onboarding = createUserOnboarding(user);
            UserNotificationSetting notificationSetting = createNotificationSetting(user);

            given(userQueryService.getUser(userId)).willReturn(user);
            given(userQueryService.getUserOnboarding(userId)).willReturn(onboarding);
            given(userNotificationSettingRepository.findByUserId(userId)).willReturn(Optional.of(notificationSetting));

            LocalTime wakeUpTime = LocalTime.of(7, 30);
            LocalTime bedTime = LocalTime.of(23, 0);

            // when
            OnboardingResponse response = onboardingService.updateSleepSchedule(userId, wakeUpTime, bedTime);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getWakeUpTime()).isEqualTo(wakeUpTime);
            assertThat(response.getBedTime()).isEqualTo(bedTime);
        }

        @Test
        @DisplayName("성공 - null 값으로 기상/취침 시간을 초기화한다")
        void success_withNullTimes() {
            // given
            User user = createCompletedUser();
            UserOnboarding onboarding = createUserOnboardingWithSleepSchedule(
                    user, LocalTime.of(7, 0), LocalTime.of(23, 0)
            );
            UserNotificationSetting notificationSetting = createNotificationSetting(user);

            given(userQueryService.getUser(userId)).willReturn(user);
            given(userQueryService.getUserOnboarding(userId)).willReturn(onboarding);
            given(userNotificationSettingRepository.findByUserId(userId)).willReturn(Optional.of(notificationSetting));

            // when
            OnboardingResponse response = onboardingService.updateSleepSchedule(userId, null, null);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getWakeUpTime()).isNull();
            assertThat(response.getBedTime()).isNull();
        }

        @Test
        @DisplayName("실패 - 온보딩 미완료 상태에서 수정 시 ONBOARDING_NOT_COMPLETED 예외")
        void fail_onboardingNotCompleted() {
            // given
            User user = createIncompleteUser();
            given(userQueryService.getUser(userId)).willReturn(user);

            // when & then
            assertThatThrownBy(() -> onboardingService.updateSleepSchedule(
                    userId, LocalTime.of(7, 0), LocalTime.of(23, 0)))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ONBOARDING_NOT_COMPLETED);
        }
    }

    /* ======================== */
    /* ===== Helper Zone ====== */
    /* ======================== */

    private User createIncompleteUser() {
        return User.builder()
                .userId(userId)
                .email("test@example.com")
                .build();
    }

    private User createCompletedUser() {
        return User.builder()
                .userId(userId)
                .email("test@example.com")
                .nickname("테스트유저")
                .onboardingCompleted(true)
                .build();
    }

    private UserOnboarding createUserOnboarding(User user) {
        return UserOnboarding.builder()
                .user(user)
                .appGoalText("체중 감량")
                .workTimeType(WorkTimeType.FIXED)
                .availableStartTime(LocalTime.of(18, 0))
                .availableEndTime(LocalTime.of(21, 0))
                .minExerciseMinutes(30)
                .preferredExercises(List.of("스트레칭", "걷기"))
                .lifestyleType(LifestyleType.REGULAR_DAYTIME)
                .build();
    }

    private UserOnboarding createUserOnboardingWithSleepSchedule(
            User user, LocalTime wakeUpTime, LocalTime bedTime) {
        return UserOnboarding.builder()
                .user(user)
                .appGoalText("체중 감량")
                .workTimeType(WorkTimeType.FIXED)
                .availableStartTime(LocalTime.of(18, 0))
                .availableEndTime(LocalTime.of(21, 0))
                .minExerciseMinutes(30)
                .preferredExercises(List.of("스트레칭", "걷기"))
                .lifestyleType(LifestyleType.REGULAR_DAYTIME)
                .wakeUpTime(wakeUpTime)
                .bedTime(bedTime)
                .build();
    }

    private UserNotificationSetting createNotificationSetting(User user) {
        return UserNotificationSetting.builder()
                .user(user)
                .remindEnabled(true)
                .checkinEnabled(true)
                .reviewEnabled(true)
                .build();
    }

    private OnboardingRequest createOnboardingRequest(LocalTime wakeUpTime, LocalTime bedTime) {
        OnboardingRequest request = new OnboardingRequest();
        request.setNickname("테스트유저");
        request.setAppGoalText("체중 감량");
        request.setWorkTimeType(WorkTimeType.FIXED);
        request.setAvailableStartTime(LocalTime.of(18, 0));
        request.setAvailableEndTime(LocalTime.of(21, 0));
        request.setMinExerciseMinutes(30);
        request.setPreferredExercises(List.of("스트레칭", "걷기"));
        request.setLifestyleType(LifestyleType.REGULAR_DAYTIME);
        request.setRemindEnabled(true);
        request.setCheckinEnabled(true);
        request.setReviewEnabled(true);
        request.setWakeUpTime(wakeUpTime);
        request.setBedTime(bedTime);
        return request;
    }
}
