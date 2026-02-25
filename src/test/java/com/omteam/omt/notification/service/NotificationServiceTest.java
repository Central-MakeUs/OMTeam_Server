package com.omteam.omt.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import com.omteam.omt.common.exception.BusinessException;
import com.omteam.omt.common.exception.ErrorCode;
import com.omteam.omt.mission.domain.RecommendedMissionStatus;
import com.omteam.omt.mission.repository.DailyRecommendedMissionRepository;
import com.omteam.omt.notification.constant.NotificationMessages;
import com.omteam.omt.report.service.DailyAnalysisService;
import com.omteam.omt.user.domain.LifestyleType;
import com.omteam.omt.user.domain.User;
import com.omteam.omt.user.domain.UserNotificationSetting;
import com.omteam.omt.user.domain.UserOnboarding;
import com.omteam.omt.user.domain.WorkTimeType;
import com.omteam.omt.user.repository.UserNotificationSettingRepository;
import com.omteam.omt.user.repository.UserOnboardingRepository;
import com.omteam.omt.user.service.UserQueryService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("[단위] NotificationService")
class NotificationServiceTest {

    @Mock
    UserQueryService userQueryService;

    @Mock
    UserOnboardingRepository userOnboardingRepository;

    @Mock
    UserNotificationSettingRepository notificationSettingRepository;

    @Mock
    DailyRecommendedMissionRepository recommendedMissionRepository;

    @Mock
    DailyAnalysisService dailyAnalysisService;

    @Mock
    FcmService fcmService;

    @InjectMocks
    NotificationService notificationService;

    final Long userId = 1L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notificationService, "defaultWakeUpTimeStr", "07:00");
        ReflectionTestUtils.setField(notificationService, "defaultBedTimeStr", "23:00");
        ReflectionTestUtils.setField(notificationService, "defaultWakeUpTime", LocalTime.of(7, 0));
        ReflectionTestUtils.setField(notificationService, "defaultBedTime", LocalTime.of(23, 0));
    }

    // =========================================================
    // registerFcmToken
    // =========================================================

    @Nested
    @DisplayName("registerFcmToken - FCM 토큰 등록/갱신")
    class RegisterFcmToken {

        @Test
        @DisplayName("성공 - FCM 토큰이 사용자에게 저장된다")
        void success() {
            // given
            User user = createUser();
            given(userQueryService.getUser(userId)).willReturn(user);

            // when
            notificationService.registerFcmToken(userId, "new-fcm-token");

            // then
            assertThat(user.getFcmToken()).isEqualTo("new-fcm-token");
            then(userQueryService).should().getUser(userId);
        }

        @Test
        @DisplayName("성공 - 기존 토큰이 있어도 새 토큰으로 갱신된다")
        void success_updateExistingToken() {
            // given
            User user = createUserWithFcmToken("old-token");
            given(userQueryService.getUser(userId)).willReturn(user);

            // when
            notificationService.registerFcmToken(userId, "new-token");

            // then
            assertThat(user.getFcmToken()).isEqualTo("new-token");
        }

        @Test
        @DisplayName("실패 - 사용자를 찾을 수 없으면 USER_NOT_FOUND 예외")
        void fail_userNotFound() {
            // given
            given(userQueryService.getUser(userId))
                    .willThrow(new BusinessException(ErrorCode.USER_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> notificationService.registerFcmToken(userId, "token"))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }
    }

    // =========================================================
    // deleteFcmToken
    // =========================================================

    @Nested
    @DisplayName("deleteFcmToken - FCM 토큰 삭제")
    class DeleteFcmToken {

        @Test
        @DisplayName("성공 - FCM 토큰이 null로 초기화된다")
        void success() {
            // given
            User user = createUserWithFcmToken("existing-token");
            given(userQueryService.getUser(userId)).willReturn(user);

            // when
            notificationService.deleteFcmToken(userId);

            // then
            assertThat(user.getFcmToken()).isNull();
            then(userQueryService).should().getUser(userId);
        }

        @Test
        @DisplayName("성공 - 토큰이 없어도 예외 없이 null로 설정된다")
        void success_noExistingToken() {
            // given
            User user = createUser();
            given(userQueryService.getUser(userId)).willReturn(user);

            // when
            notificationService.deleteFcmToken(userId);

            // then
            assertThat(user.getFcmToken()).isNull();
        }

        @Test
        @DisplayName("실패 - 사용자를 찾을 수 없으면 USER_NOT_FOUND 예외")
        void fail_userNotFound() {
            // given
            given(userQueryService.getUser(userId))
                    .willThrow(new BusinessException(ErrorCode.USER_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> notificationService.deleteFcmToken(userId))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }
    }

    // =========================================================
    // sendRemindNotifications
    // =========================================================

    @Nested
    @DisplayName("sendRemindNotifications - 리마인드 알림 발송")
    class SendRemindNotifications {

        final LocalTime now = LocalTime.of(9, 0);
        final LocalDate today = LocalDate.of(2026, 2, 25);

        @Test
        @DisplayName("성공 - 조건 충족 사용자에게 리마인드 알림이 발송된다")
        void success() {
            // given
            User user = createUserWithFcmToken("fcm-token");
            UserOnboarding onboarding = createOnboarding(user, now);
            UserNotificationSetting setting = createSetting(user, true, true, true);

            given(userOnboardingRepository.findByAvailableStartTimeForNotification(now))
                    .willReturn(List.of(onboarding));
            given(notificationSettingRepository.findAllById(List.of(userId)))
                    .willReturn(List.of(setting));
            given(recommendedMissionRepository.findUserIdsHavingActiveMissions(anyList(), eq(today), anyList()))
                    .willReturn(Set.of());

            // when
            notificationService.sendRemindNotifications(now, today);

            // then
            then(fcmService).should().sendNotification(
                    "fcm-token",
                    NotificationMessages.REMIND_TITLE,
                    NotificationMessages.REMIND_BODY);
        }

        @Test
        @DisplayName("건너뜀 - remindEnabled=false인 경우 알림을 발송하지 않는다")
        void skip_remindDisabled() {
            // given
            User user = createUserWithFcmToken("fcm-token");
            UserOnboarding onboarding = createOnboarding(user, now);
            UserNotificationSetting setting = createSetting(user, false, true, true);

            given(userOnboardingRepository.findByAvailableStartTimeForNotification(now))
                    .willReturn(List.of(onboarding));
            given(notificationSettingRepository.findAllById(List.of(userId)))
                    .willReturn(List.of(setting));
            given(recommendedMissionRepository.findUserIdsHavingActiveMissions(anyList(), eq(today), anyList()))
                    .willReturn(Set.of());

            // when
            notificationService.sendRemindNotifications(now, today);

            // then
            then(fcmService).should(never()).sendNotification(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("건너뜀 - FCM 토큰이 없는 경우 알림을 발송하지 않는다")
        void skip_noFcmToken() {
            // given
            User user = createUser();
            UserOnboarding onboarding = createOnboarding(user, now);
            UserNotificationSetting setting = createSetting(user, true, true, true);

            given(userOnboardingRepository.findByAvailableStartTimeForNotification(now))
                    .willReturn(List.of(onboarding));
            given(notificationSettingRepository.findAllById(List.of(userId)))
                    .willReturn(List.of(setting));
            given(recommendedMissionRepository.findUserIdsHavingActiveMissions(anyList(), eq(today), anyList()))
                    .willReturn(Set.of());

            // when
            notificationService.sendRemindNotifications(now, today);

            // then
            then(fcmService).should(never()).sendNotification(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("건너뜀 - 오늘 IN_PROGRESS 미션이 있으면 알림을 발송하지 않는다")
        void skip_inProgressMissionExists() {
            // given
            User user = createUserWithFcmToken("fcm-token");
            UserOnboarding onboarding = createOnboarding(user, now);
            UserNotificationSetting setting = createSetting(user, true, true, true);

            given(userOnboardingRepository.findByAvailableStartTimeForNotification(now))
                    .willReturn(List.of(onboarding));
            given(notificationSettingRepository.findAllById(List.of(userId)))
                    .willReturn(List.of(setting));
            given(recommendedMissionRepository.findUserIdsHavingActiveMissions(anyList(), eq(today), anyList()))
                    .willReturn(Set.of(userId));

            // when
            notificationService.sendRemindNotifications(now, today);

            // then
            then(fcmService).should(never()).sendNotification(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("건너뜀 - 오늘 COMPLETED 미션이 있으면 알림을 발송하지 않는다")
        void skip_completedMissionExists() {
            // given
            User user = createUserWithFcmToken("fcm-token");
            UserOnboarding onboarding = createOnboarding(user, now);
            UserNotificationSetting setting = createSetting(user, true, true, true);

            given(userOnboardingRepository.findByAvailableStartTimeForNotification(now))
                    .willReturn(List.of(onboarding));
            given(notificationSettingRepository.findAllById(List.of(userId)))
                    .willReturn(List.of(setting));
            given(recommendedMissionRepository.findUserIdsHavingActiveMissions(anyList(), eq(today), anyList()))
                    .willReturn(Set.of(userId));

            // when
            notificationService.sendRemindNotifications(now, today);

            // then
            then(fcmService).should(never()).sendNotification(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("조건 충족 사용자가 없으면 아무것도 하지 않는다")
        void noTargets_doNothing() {
            // given
            given(userOnboardingRepository.findByAvailableStartTimeForNotification(now))
                    .willReturn(List.of());

            // when
            notificationService.sendRemindNotifications(now, today);

            // then
            then(notificationSettingRepository).should(never()).findAllById(anyList());
            then(fcmService).should(never()).sendNotification(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("FCM 전송 실패해도 다음 사용자 처리를 계속한다")
        void fcmFailure_continuesNextUser() {
            // given
            User user1 = createUserWithFcmToken("token-1");
            User user2 = User.builder()
                    .userId(2L)
                    .email("user2@example.com")
                    .fcmToken("token-2")
                    .build();

            UserOnboarding onboarding1 = createOnboarding(user1, now);
            UserOnboarding onboarding2 = UserOnboarding.builder()
                    .userId(2L)
                    .user(user2)
                    .availableStartTime(now)
                    .availableEndTime(now.plusHours(1))
                    .appGoalText("목표")
                    .workTimeType(WorkTimeType.FIXED)
                    .minExerciseMinutes(30)
                    .preferredExercises(List.of("달리기"))
                    .lifestyleType(LifestyleType.REGULAR_DAYTIME)
                    .build();

            UserNotificationSetting setting1 = createSetting(user1, true, true, true);
            UserNotificationSetting setting2 = UserNotificationSetting.builder()
                    .userId(2L)
                    .user(user2)
                    .remindEnabled(true)
                    .checkinEnabled(true)
                    .reviewEnabled(true)
                    .build();

            given(userOnboardingRepository.findByAvailableStartTimeForNotification(now))
                    .willReturn(List.of(onboarding1, onboarding2));
            given(notificationSettingRepository.findAllById(List.of(userId, 2L)))
                    .willReturn(List.of(setting1, setting2));
            given(recommendedMissionRepository.findUserIdsHavingActiveMissions(anyList(), eq(today), anyList()))
                    .willReturn(Set.of());

            willThrow(new RuntimeException("FCM 오류"))
                    .given(fcmService).sendNotification(eq("token-1"), anyString(), anyString());
            willDoNothing()
                    .given(fcmService).sendNotification(eq("token-2"), anyString(), anyString());

            // when
            notificationService.sendRemindNotifications(now, today);

            // then
            then(fcmService).should().sendNotification(eq("token-1"), anyString(), anyString());
            then(fcmService).should().sendNotification(eq("token-2"), anyString(), anyString());
        }
    }

    // =========================================================
    // sendCheckinNotifications
    // =========================================================

    @Nested
    @DisplayName("sendCheckinNotifications - 체크인 알림 발송")
    class SendCheckinNotifications {

        final LocalTime now = LocalTime.of(7, 0);
        final LocalTime defaultWakeUpTime = LocalTime.of(7, 0);

        @Test
        @DisplayName("성공 - 조건 충족 사용자에게 체크인 알림이 발송된다")
        void success() {
            // given
            User user = createUserWithFcmToken("fcm-token");
            UserOnboarding onboarding = createOnboardingWithWakeUpTime(user, now);
            UserNotificationSetting setting = createSetting(user, true, true, true);

            given(userOnboardingRepository.findByEffectiveWakeUpTime(now, defaultWakeUpTime))
                    .willReturn(List.of(onboarding));
            given(notificationSettingRepository.findAllById(List.of(userId)))
                    .willReturn(List.of(setting));

            // when
            notificationService.sendCheckinNotifications(now);

            // then
            then(fcmService).should().sendNotification(
                    "fcm-token",
                    NotificationMessages.CHECKIN_TITLE,
                    NotificationMessages.CHECKIN_BODY);
        }

        @Test
        @DisplayName("건너뜀 - checkinEnabled=false인 경우 알림을 발송하지 않는다")
        void skip_checkinDisabled() {
            // given
            User user = createUserWithFcmToken("fcm-token");
            UserOnboarding onboarding = createOnboardingWithWakeUpTime(user, now);
            UserNotificationSetting setting = createSetting(user, true, false, true);

            given(userOnboardingRepository.findByEffectiveWakeUpTime(now, defaultWakeUpTime))
                    .willReturn(List.of(onboarding));
            given(notificationSettingRepository.findAllById(List.of(userId)))
                    .willReturn(List.of(setting));

            // when
            notificationService.sendCheckinNotifications(now);

            // then
            then(fcmService).should(never()).sendNotification(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("건너뜀 - FCM 토큰이 없는 경우 알림을 발송하지 않는다")
        void skip_noFcmToken() {
            // given
            User user = createUser();
            UserOnboarding onboarding = createOnboardingWithWakeUpTime(user, now);
            UserNotificationSetting setting = createSetting(user, true, true, true);

            given(userOnboardingRepository.findByEffectiveWakeUpTime(now, defaultWakeUpTime))
                    .willReturn(List.of(onboarding));
            given(notificationSettingRepository.findAllById(List.of(userId)))
                    .willReturn(List.of(setting));

            // when
            notificationService.sendCheckinNotifications(now);

            // then
            then(fcmService).should(never()).sendNotification(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("조건 충족 사용자가 없으면 아무것도 하지 않는다")
        void noTargets_doNothing() {
            // given
            given(userOnboardingRepository.findByEffectiveWakeUpTime(now, defaultWakeUpTime))
                    .willReturn(List.of());

            // when
            notificationService.sendCheckinNotifications(now);

            // then
            then(notificationSettingRepository).should(never()).findAllById(anyList());
            then(fcmService).should(never()).sendNotification(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("wakeUpTime=null인 사용자도 기본값(defaultWakeUpTime)으로 조회된다")
        void nullWakeUpTime_usesDefaultTime() {
            // given
            User user = createUserWithFcmToken("fcm-token");
            UserOnboarding onboarding = createOnboarding(user, LocalTime.of(9, 0)); // wakeUpTime=null
            UserNotificationSetting setting = createSetting(user, true, true, true);

            given(userOnboardingRepository.findByEffectiveWakeUpTime(defaultWakeUpTime, defaultWakeUpTime))
                    .willReturn(List.of(onboarding));
            given(notificationSettingRepository.findAllById(List.of(userId)))
                    .willReturn(List.of(setting));

            // when
            notificationService.sendCheckinNotifications(defaultWakeUpTime);

            // then
            then(fcmService).should().sendNotification(
                    "fcm-token",
                    NotificationMessages.CHECKIN_TITLE,
                    NotificationMessages.CHECKIN_BODY);
        }
    }

    // =========================================================
    // sendReviewNotifications
    // =========================================================

    @Nested
    @DisplayName("sendReviewNotifications - 회고 알림 발송")
    class SendReviewNotifications {

        // defaultBedTimeStr="23:00" => defaultReviewTime=22:00
        // now=22:00 => targetBedTime=23:00
        final LocalTime now = LocalTime.of(22, 0);
        final LocalTime targetBedTime = LocalTime.of(23, 0);
        final LocalTime defaultReviewTime = LocalTime.of(22, 0);
        final LocalDate today = LocalDate.of(2026, 2, 25);

        @Test
        @DisplayName("성공 - 분석 생성 후 회고 알림이 발송된다")
        void success() {
            // given
            User user = createUserWithFcmToken("fcm-token");
            UserOnboarding onboarding = createOnboardingWithBedTime(user, targetBedTime);
            UserNotificationSetting setting = createSetting(user, true, true, true);

            given(userOnboardingRepository.findByBedTimeForReview(targetBedTime, now, defaultReviewTime))
                    .willReturn(List.of(onboarding));
            given(notificationSettingRepository.findAllById(List.of(userId)))
                    .willReturn(List.of(setting));
            willDoNothing()
                    .given(dailyAnalysisService).generateDailyAnalysisForUser(eq(user), eq(today));

            // when
            notificationService.sendReviewNotifications(now, today);

            // then
            then(dailyAnalysisService).should().generateDailyAnalysisForUser(eq(user), eq(today));
            then(fcmService).should().sendNotification(
                    "fcm-token",
                    NotificationMessages.REVIEW_TITLE,
                    NotificationMessages.REVIEW_BODY);
        }

        @Test
        @DisplayName("건너뜀 - reviewEnabled=false인 경우 알림을 발송하지 않는다")
        void skip_reviewDisabled() {
            // given
            User user = createUserWithFcmToken("fcm-token");
            UserOnboarding onboarding = createOnboardingWithBedTime(user, targetBedTime);
            UserNotificationSetting setting = createSetting(user, true, true, false);

            given(userOnboardingRepository.findByBedTimeForReview(targetBedTime, now, defaultReviewTime))
                    .willReturn(List.of(onboarding));
            given(notificationSettingRepository.findAllById(List.of(userId)))
                    .willReturn(List.of(setting));

            // when
            notificationService.sendReviewNotifications(now, today);

            // then
            then(dailyAnalysisService).should(never()).generateDailyAnalysisForUser(any(), any());
            then(fcmService).should(never()).sendNotification(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("건너뜀 - FCM 토큰이 없는 경우 알림을 발송하지 않는다")
        void skip_noFcmToken() {
            // given
            User user = createUser();
            UserOnboarding onboarding = createOnboardingWithBedTime(user, targetBedTime);
            UserNotificationSetting setting = createSetting(user, true, true, true);

            given(userOnboardingRepository.findByBedTimeForReview(targetBedTime, now, defaultReviewTime))
                    .willReturn(List.of(onboarding));
            given(notificationSettingRepository.findAllById(List.of(userId)))
                    .willReturn(List.of(setting));

            // when
            notificationService.sendReviewNotifications(now, today);

            // then
            then(dailyAnalysisService).should(never()).generateDailyAnalysisForUser(any(), any());
            then(fcmService).should(never()).sendNotification(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("조건 충족 사용자가 없으면 아무것도 하지 않는다")
        void noTargets_doNothing() {
            // given
            given(userOnboardingRepository.findByBedTimeForReview(targetBedTime, now, defaultReviewTime))
                    .willReturn(List.of());

            // when
            notificationService.sendReviewNotifications(now, today);

            // then
            then(notificationSettingRepository).should(never()).findAllById(anyList());
            then(dailyAnalysisService).should(never()).generateDailyAnalysisForUser(any(), any());
            then(fcmService).should(never()).sendNotification(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("분석 생성 실패 시 예외를 무시하고 FCM 알림도 발송하지 않는다")
        void analysisFailure_exceptionIgnoredAndFcmNotSent() {
            // given
            User user = createUserWithFcmToken("fcm-token");
            UserOnboarding onboarding = createOnboardingWithBedTime(user, targetBedTime);
            UserNotificationSetting setting = createSetting(user, true, true, true);

            given(userOnboardingRepository.findByBedTimeForReview(targetBedTime, now, defaultReviewTime))
                    .willReturn(List.of(onboarding));
            given(notificationSettingRepository.findAllById(List.of(userId)))
                    .willReturn(List.of(setting));
            willThrow(new RuntimeException("AI 분석 실패"))
                    .given(dailyAnalysisService).generateDailyAnalysisForUser(eq(user), eq(today));

            // when
            notificationService.sendReviewNotifications(now, today);

            // then: catch 블록에서 예외를 삼키므로 FCM도 호출되지 않는다
            then(fcmService).should(never()).sendNotification(anyString(), anyString(), anyString());
        }
    }

    /* ======================== */
    /* ===== Helper Zone ====== */
    /* ======================== */

    private User createUser() {
        return User.builder()
                .userId(userId)
                .email("test@example.com")
                .build();
    }

    private User createUserWithFcmToken(String fcmToken) {
        return User.builder()
                .userId(userId)
                .email("test@example.com")
                .fcmToken(fcmToken)
                .build();
    }

    private UserOnboarding createOnboarding(User user, LocalTime availableStartTime) {
        return UserOnboarding.builder()
                .userId(user.getUserId())
                .user(user)
                .availableStartTime(availableStartTime)
                .availableEndTime(availableStartTime.plusHours(1))
                .appGoalText("목표")
                .workTimeType(WorkTimeType.FIXED)
                .minExerciseMinutes(30)
                .preferredExercises(List.of("달리기"))
                .lifestyleType(LifestyleType.REGULAR_DAYTIME)
                .build();
    }

    private UserOnboarding createOnboardingWithWakeUpTime(User user, LocalTime wakeUpTime) {
        return UserOnboarding.builder()
                .userId(user.getUserId())
                .user(user)
                .availableStartTime(LocalTime.of(9, 0))
                .availableEndTime(LocalTime.of(10, 0))
                .appGoalText("목표")
                .workTimeType(WorkTimeType.FIXED)
                .minExerciseMinutes(30)
                .preferredExercises(List.of("달리기"))
                .lifestyleType(LifestyleType.REGULAR_DAYTIME)
                .wakeUpTime(wakeUpTime)
                .build();
    }

    private UserOnboarding createOnboardingWithBedTime(User user, LocalTime bedTime) {
        return UserOnboarding.builder()
                .userId(user.getUserId())
                .user(user)
                .availableStartTime(LocalTime.of(9, 0))
                .availableEndTime(LocalTime.of(10, 0))
                .appGoalText("목표")
                .workTimeType(WorkTimeType.FIXED)
                .minExerciseMinutes(30)
                .preferredExercises(List.of("달리기"))
                .lifestyleType(LifestyleType.REGULAR_DAYTIME)
                .bedTime(bedTime)
                .build();
    }

    private UserNotificationSetting createSetting(
            User user, boolean remindEnabled, boolean checkinEnabled, boolean reviewEnabled) {
        return UserNotificationSetting.builder()
                .userId(user.getUserId())
                .user(user)
                .remindEnabled(remindEnabled)
                .checkinEnabled(checkinEnabled)
                .reviewEnabled(reviewEnabled)
                .build();
    }
}
