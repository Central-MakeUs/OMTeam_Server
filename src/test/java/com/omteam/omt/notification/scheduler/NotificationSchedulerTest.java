package com.omteam.omt.notification.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;

import com.omteam.omt.notification.service.NotificationService;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("[단위] NotificationScheduler")
class NotificationSchedulerTest {

    @Mock
    NotificationService notificationService;

    @InjectMocks
    NotificationScheduler notificationScheduler;

    @Nested
    @DisplayName("sendScheduledNotifications - 알림 스케줄러 실행")
    class SendScheduledNotifications {

        @Test
        @DisplayName("3가지 알림 모두 정상 호출된다")
        void success_allNotificationsCalled() {
            // given
            willDoNothing().given(notificationService).sendRemindNotifications(any(LocalTime.class), any(LocalDate.class));
            willDoNothing().given(notificationService).sendCheckinNotifications(any(LocalTime.class));
            willDoNothing().given(notificationService).sendReviewNotifications(any(LocalTime.class), any(LocalDate.class));

            // when
            notificationScheduler.sendScheduledNotifications();

            // then
            then(notificationService).should().sendRemindNotifications(any(LocalTime.class), any(LocalDate.class));
            then(notificationService).should().sendCheckinNotifications(any(LocalTime.class));
            then(notificationService).should().sendReviewNotifications(any(LocalTime.class), any(LocalDate.class));
        }

        @Test
        @DisplayName("리마인드 알림 실패해도 체크인, 회고 알림이 계속 실행된다")
        void remindFails_checkinAndReviewStillCalled() {
            // given
            willThrow(new RuntimeException("리마인드 알림 오류"))
                    .given(notificationService).sendRemindNotifications(any(LocalTime.class), any(LocalDate.class));
            willDoNothing().given(notificationService).sendCheckinNotifications(any(LocalTime.class));
            willDoNothing().given(notificationService).sendReviewNotifications(any(LocalTime.class), any(LocalDate.class));

            // when
            notificationScheduler.sendScheduledNotifications();

            // then
            then(notificationService).should().sendCheckinNotifications(any(LocalTime.class));
            then(notificationService).should().sendReviewNotifications(any(LocalTime.class), any(LocalDate.class));
        }

        @Test
        @DisplayName("체크인 알림 실패해도 회고 알림이 계속 실행된다")
        void checkinFails_reviewStillCalled() {
            // given
            willDoNothing().given(notificationService).sendRemindNotifications(any(LocalTime.class), any(LocalDate.class));
            willThrow(new RuntimeException("체크인 알림 오류"))
                    .given(notificationService).sendCheckinNotifications(any(LocalTime.class));
            willDoNothing().given(notificationService).sendReviewNotifications(any(LocalTime.class), any(LocalDate.class));

            // when
            notificationScheduler.sendScheduledNotifications();

            // then
            then(notificationService).should().sendReviewNotifications(any(LocalTime.class), any(LocalDate.class));
        }
    }
}
