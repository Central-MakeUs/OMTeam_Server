package com.omteam.omt.onboarding.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

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
import com.omteam.omt.onboarding.dto.request.UpdateWorkTimeRequest;
import com.omteam.omt.onboarding.service.OnboardingService;
import com.omteam.omt.security.principal.UserPrincipal;
import com.omteam.omt.user.domain.LifestyleType;
import com.omteam.omt.user.domain.NotificationType;
import com.omteam.omt.user.domain.WorkTimeType;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("[단위] OnboardingController")
class OnboardingControllerTest {

    @Mock
    OnboardingService onboardingService;

    @InjectMocks
    OnboardingController onboardingController;

    UserPrincipal principal;

    @BeforeEach
    void setUp() {
        principal = new UserPrincipal(1L);
    }

    @Nested
    @DisplayName("온보딩 정보 등록")
    class CreateOnboarding {

        @Test
        @DisplayName("성공 - 온보딩 정보가 등록된다")
        void success() {
            // given
            OnboardingRequest request = createOnboardingRequest();
            OnboardingResponse response = createOnboardingResponse();

            given(onboardingService.createOnboarding(principal.userId(), request))
                    .willReturn(response);

            // when
            ApiResponse<OnboardingResponse> result =
                    onboardingController.createOnboarding(principal, request);

            // then
            assertThat(result.success()).isTrue();
            assertThat(result.data()).isNotNull();
            assertThat(result.data().getNickname()).isEqualTo("테스트유저");
            assertThat(result.data().getAppGoalText()).isEqualTo("체중 감량");

            then(onboardingService).should().createOnboarding(principal.userId(), request);
        }
    }

    @Nested
    @DisplayName("온보딩 정보 수정")
    class UpdateOnboarding {

        @Test
        @DisplayName("성공 - 온보딩 정보가 수정된다")
        void success() {
            // given
            OnboardingRequest request = createOnboardingRequest();
            OnboardingResponse response = createOnboardingResponse();

            given(onboardingService.updateOnboarding(principal.userId(), request))
                    .willReturn(response);

            // when
            ApiResponse<OnboardingResponse> result =
                    onboardingController.updateOnboarding(principal, request);

            // then
            assertThat(result.success()).isTrue();
            assertThat(result.data()).isNotNull();

            then(onboardingService).should().updateOnboarding(principal.userId(), request);
        }
    }

    @Nested
    @DisplayName("온보딩 정보 조회")
    class GetOnboarding {

        @Test
        @DisplayName("성공 - 온보딩 정보가 조회된다")
        void success() {
            // given
            OnboardingResponse response = createOnboardingResponse();

            given(onboardingService.getOnboarding(principal.userId()))
                    .willReturn(response);

            // when
            ApiResponse<OnboardingResponse> result =
                    onboardingController.getOnboarding(principal);

            // then
            assertThat(result.success()).isTrue();
            assertThat(result.data()).isNotNull();
            assertThat(result.data().getNickname()).isEqualTo("테스트유저");
            assertThat(result.data().getWorkTimeType()).isEqualTo(WorkTimeType.FIXED);
            assertThat(result.data().getLifestyleType()).isEqualTo(LifestyleType.REGULAR_DAYTIME);

            then(onboardingService).should().getOnboarding(principal.userId());
        }
    }

    @Nested
    @DisplayName("닉네임 수정")
    class UpdateNickname {

        @Test
        @DisplayName("성공 - 닉네임이 수정된다")
        void success() {
            // given
            UpdateNicknameRequest request = new UpdateNicknameRequest();
            request.setNickname("새닉네임");

            OnboardingResponse response = OnboardingResponse.builder()
                    .nickname("새닉네임")
                    .appGoalText("체중 감량")
                    .workTimeType(WorkTimeType.FIXED)
                    .build();

            given(onboardingService.updateNickname(principal.userId(), "새닉네임"))
                    .willReturn(response);

            // when
            ApiResponse<OnboardingResponse> result =
                    onboardingController.updateNickname(principal, request);

            // then
            assertThat(result.success()).isTrue();
            assertThat(result.data().getNickname()).isEqualTo("새닉네임");

            then(onboardingService).should().updateNickname(principal.userId(), "새닉네임");
        }
    }

    @Nested
    @DisplayName("앱 사용 목적 수정")
    class UpdateAppGoal {

        @Test
        @DisplayName("성공 - 앱 사용 목적이 수정된다")
        void success() {
            // given
            UpdateAppGoalRequest request = new UpdateAppGoalRequest();
            request.setAppGoalText("건강 유지");

            OnboardingResponse response = createOnboardingResponseWith("appGoalText", "건강 유지");

            given(onboardingService.updateAppGoal(principal.userId(), "건강 유지"))
                    .willReturn(response);

            // when
            ApiResponse<OnboardingResponse> result =
                    onboardingController.updateAppGoal(principal, request);

            // then
            assertThat(result.success()).isTrue();
            assertThat(result.data().getAppGoalText()).isEqualTo("건강 유지");

            then(onboardingService).should().updateAppGoal(principal.userId(), "건강 유지");
        }
    }

    @Nested
    @DisplayName("근무 시간 유형 수정")
    class UpdateWorkTime {

        @Test
        @DisplayName("성공 - 근무 시간 유형이 SHIFT로 수정된다")
        void success() {
            // given
            UpdateWorkTimeRequest request = new UpdateWorkTimeRequest();
            request.setWorkTimeType(WorkTimeType.SHIFT);

            OnboardingResponse response = OnboardingResponse.builder()
                    .nickname("테스트유저")
                    .workTimeType(WorkTimeType.SHIFT)
                    .build();

            given(onboardingService.updateWorkTimeType(principal.userId(), WorkTimeType.SHIFT))
                    .willReturn(response);

            // when
            ApiResponse<OnboardingResponse> result =
                    onboardingController.updateWorkTime(principal, request);

            // then
            assertThat(result.success()).isTrue();
            assertThat(result.data().getWorkTimeType()).isEqualTo(WorkTimeType.SHIFT);

            then(onboardingService).should().updateWorkTimeType(principal.userId(), WorkTimeType.SHIFT);
        }
    }

    @Nested
    @DisplayName("운동 가능 시간대 수정")
    class UpdateAvailableTime {

        @Test
        @DisplayName("성공 - 운동 가능 시간대가 수정된다")
        void success() {
            // given
            LocalTime startTime = LocalTime.of(19, 0);
            LocalTime endTime = LocalTime.of(22, 0);

            UpdateAvailableTimeRequest request = new UpdateAvailableTimeRequest();
            request.setAvailableStartTime(startTime);
            request.setAvailableEndTime(endTime);

            OnboardingResponse response = OnboardingResponse.builder()
                    .nickname("테스트유저")
                    .availableStartTime(startTime)
                    .availableEndTime(endTime)
                    .build();

            given(onboardingService.updateAvailableTime(principal.userId(), startTime, endTime))
                    .willReturn(response);

            // when
            ApiResponse<OnboardingResponse> result =
                    onboardingController.updateAvailableTime(principal, request);

            // then
            assertThat(result.success()).isTrue();
            assertThat(result.data().getAvailableStartTime()).isEqualTo(startTime);
            assertThat(result.data().getAvailableEndTime()).isEqualTo(endTime);

            then(onboardingService).should().updateAvailableTime(principal.userId(), startTime, endTime);
        }
    }

    @Nested
    @DisplayName("최소 운동 시간 수정")
    class UpdateMinExerciseMinutes {

        @Test
        @DisplayName("성공 - 최소 운동 시간이 수정된다")
        void success() {
            // given
            UpdateMinExerciseMinutesRequest request = new UpdateMinExerciseMinutesRequest();
            request.setMinExerciseMinutes(45);

            OnboardingResponse response = OnboardingResponse.builder()
                    .nickname("테스트유저")
                    .minExerciseMinutes(45)
                    .build();

            given(onboardingService.updateMinExerciseMinutes(principal.userId(), 45))
                    .willReturn(response);

            // when
            ApiResponse<OnboardingResponse> result =
                    onboardingController.updateMinExerciseMinutes(principal, request);

            // then
            assertThat(result.success()).isTrue();
            assertThat(result.data().getMinExerciseMinutes()).isEqualTo(45);

            then(onboardingService).should().updateMinExerciseMinutes(principal.userId(), 45);
        }
    }

    @Nested
    @DisplayName("선호 운동 수정")
    class UpdatePreferredExercise {

        @Test
        @DisplayName("성공 - 선호 운동이 수정된다")
        void success() {
            // given
            List<String> exercises = List.of("요가", "필라테스", "수영");

            UpdatePreferredExerciseRequest request = new UpdatePreferredExerciseRequest();
            request.setPreferredExercises(exercises);

            OnboardingResponse response = OnboardingResponse.builder()
                    .nickname("테스트유저")
                    .preferredExercises(exercises)
                    .build();

            given(onboardingService.updatePreferredExercise(principal.userId(), exercises))
                    .willReturn(response);

            // when
            ApiResponse<OnboardingResponse> result =
                    onboardingController.updatePreferredExercise(principal, request);

            // then
            assertThat(result.success()).isTrue();
            assertThat(result.data().getPreferredExercises()).containsExactly("요가", "필라테스", "수영");

            then(onboardingService).should().updatePreferredExercise(principal.userId(), exercises);
        }
    }

    @Nested
    @DisplayName("생활 패턴 수정")
    class UpdateLifestyle {

        @Test
        @DisplayName("성공 - 생활 패턴이 수정된다")
        void success() {
            // given
            UpdateLifestyleRequest request = new UpdateLifestyleRequest();
            request.setLifestyleType(LifestyleType.IRREGULAR_OVERTIME);

            OnboardingResponse response = OnboardingResponse.builder()
                    .nickname("테스트유저")
                    .lifestyleType(LifestyleType.IRREGULAR_OVERTIME)
                    .build();

            given(onboardingService.updateLifestyleType(principal.userId(), LifestyleType.IRREGULAR_OVERTIME))
                    .willReturn(response);

            // when
            ApiResponse<OnboardingResponse> result =
                    onboardingController.updateLifestyle(principal, request);

            // then
            assertThat(result.success()).isTrue();
            assertThat(result.data().getLifestyleType()).isEqualTo(LifestyleType.IRREGULAR_OVERTIME);

            then(onboardingService).should().updateLifestyleType(principal.userId(), LifestyleType.IRREGULAR_OVERTIME);
        }
    }

    @Nested
    @DisplayName("알림 설정 전체 수정")
    class UpdateNotificationSetting {

        @Test
        @DisplayName("성공 - 모든 알림 설정이 수정된다")
        void success() {
            // given
            UpdateNotificationSettingRequest request = new UpdateNotificationSettingRequest();
            request.setRemindEnabled(true);
            request.setCheckinEnabled(false);
            request.setReviewEnabled(true);

            OnboardingResponse response = OnboardingResponse.builder()
                    .nickname("테스트유저")
                    .remindEnabled(true)
                    .checkinEnabled(false)
                    .reviewEnabled(true)
                    .build();

            given(onboardingService.updateNotificationSetting(principal.userId(), true, false, true))
                    .willReturn(response);

            // when
            ApiResponse<OnboardingResponse> result =
                    onboardingController.updateNotificationSetting(principal, request);

            // then
            assertThat(result.success()).isTrue();
            assertThat(result.data().isRemindEnabled()).isTrue();
            assertThat(result.data().isCheckinEnabled()).isFalse();
            assertThat(result.data().isReviewEnabled()).isTrue();

            then(onboardingService).should().updateNotificationSetting(principal.userId(), true, false, true);
        }
    }

    @Nested
    @DisplayName("개별 알림 설정 수정")
    class UpdateSingleNotification {

        @Test
        @DisplayName("성공 - REMIND 알림 활성화")
        void successRemind() {
            // given
            UpdateSingleNotificationRequest request = new UpdateSingleNotificationRequest();
            request.setEnabled(true);

            OnboardingResponse response = OnboardingResponse.builder()
                    .nickname("테스트유저")
                    .remindEnabled(true)
                    .build();

            given(onboardingService.updateSingleNotification(principal.userId(), NotificationType.REMIND, true))
                    .willReturn(response);

            // when
            ApiResponse<OnboardingResponse> result =
                    onboardingController.updateSingleNotification(principal, NotificationType.REMIND, request);

            // then
            assertThat(result.success()).isTrue();
            assertThat(result.data().isRemindEnabled()).isTrue();

            then(onboardingService).should().updateSingleNotification(principal.userId(), NotificationType.REMIND, true);
        }

        @Test
        @DisplayName("성공 - CHECKIN 알림 비활성화")
        void successCheckin() {
            // given
            UpdateSingleNotificationRequest request = new UpdateSingleNotificationRequest();
            request.setEnabled(false);

            OnboardingResponse response = OnboardingResponse.builder()
                    .nickname("테스트유저")
                    .checkinEnabled(false)
                    .build();

            given(onboardingService.updateSingleNotification(principal.userId(), NotificationType.CHECKIN, false))
                    .willReturn(response);

            // when
            ApiResponse<OnboardingResponse> result =
                    onboardingController.updateSingleNotification(principal, NotificationType.CHECKIN, request);

            // then
            assertThat(result.success()).isTrue();

            then(onboardingService).should().updateSingleNotification(principal.userId(), NotificationType.CHECKIN, false);
        }

        @Test
        @DisplayName("성공 - REVIEW 알림 활성화")
        void successReview() {
            // given
            UpdateSingleNotificationRequest request = new UpdateSingleNotificationRequest();
            request.setEnabled(true);

            OnboardingResponse response = OnboardingResponse.builder()
                    .nickname("테스트유저")
                    .reviewEnabled(true)
                    .build();

            given(onboardingService.updateSingleNotification(principal.userId(), NotificationType.REVIEW, true))
                    .willReturn(response);

            // when
            ApiResponse<OnboardingResponse> result =
                    onboardingController.updateSingleNotification(principal, NotificationType.REVIEW, request);

            // then
            assertThat(result.success()).isTrue();

            then(onboardingService).should().updateSingleNotification(principal.userId(), NotificationType.REVIEW, true);
        }
    }

    // ===== 헬퍼 메서드 =====

    private OnboardingRequest createOnboardingRequest() {
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
        return request;
    }

    private OnboardingResponse createOnboardingResponse() {
        return OnboardingResponse.builder()
                .nickname("테스트유저")
                .appGoalText("체중 감량")
                .workTimeType(WorkTimeType.FIXED)
                .availableStartTime(LocalTime.of(18, 0))
                .availableEndTime(LocalTime.of(21, 0))
                .minExerciseMinutes(30)
                .preferredExercises(List.of("스트레칭", "걷기"))
                .lifestyleType(LifestyleType.REGULAR_DAYTIME)
                .remindEnabled(true)
                .checkinEnabled(true)
                .reviewEnabled(true)
                .build();
    }

    private OnboardingResponse createOnboardingResponseWith(String field, String value) {
        OnboardingResponse.OnboardingResponseBuilder builder = OnboardingResponse.builder()
                .nickname("테스트유저")
                .appGoalText("체중 감량")
                .workTimeType(WorkTimeType.FIXED)
                .availableStartTime(LocalTime.of(18, 0))
                .availableEndTime(LocalTime.of(21, 0))
                .minExerciseMinutes(30)
                .preferredExercises(List.of("스트레칭", "걷기"))
                .lifestyleType(LifestyleType.REGULAR_DAYTIME)
                .remindEnabled(true)
                .checkinEnabled(true)
                .reviewEnabled(true);

        if ("appGoalText".equals(field)) {
            builder.appGoalText(value);
        }

        return builder.build();
    }
}
