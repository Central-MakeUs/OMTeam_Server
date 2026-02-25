package com.omteam.omt.notification.scheduler;

import com.omteam.omt.notification.service.NotificationService;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final NotificationService notificationService;

    @Scheduled(cron = "${notification.scheduler.cron}")
    public void sendScheduledNotifications() {
        LocalTime now = LocalTime.now().truncatedTo(ChronoUnit.MINUTES);
        log.info("알림 스케줄러 시작: time={}", now);

        try {
            notificationService.sendRemindNotifications(now);
        } catch (Exception e) {
            log.error("리마인드 알림 스케줄러 오류", e);
        }

        try {
            notificationService.sendCheckinNotifications(now);
        } catch (Exception e) {
            log.error("체크인 알림 스케줄러 오류", e);
        }

        try {
            notificationService.sendReviewNotifications(now);
        } catch (Exception e) {
            log.error("회고 알림 스케줄러 오류", e);
        }

        log.info("알림 스케줄러 완료");
    }
}
