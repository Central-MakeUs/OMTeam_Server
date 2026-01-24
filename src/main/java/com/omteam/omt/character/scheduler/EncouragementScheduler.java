package com.omteam.omt.character.scheduler;

import com.omteam.omt.character.service.EncouragementService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EncouragementScheduler {

    private final EncouragementService encouragementService;

    /**
     * 매일 00:30에 오늘의 격려 메시지를 생성한다.
     * (00:00에 미션 만료 스케줄러가 돌기 때문에 30분 후에 실행)
     */
    @Scheduled(cron = "0 30 0 * * *")
    public void generateDailyEncouragement() {
        LocalDate today = LocalDate.now();
        log.info("격려 메시지 스케줄러 시작: targetDate={}", today);

        try {
            int successCount = encouragementService.generateDailyEncouragementForAllUsers(today);
            log.info("격려 메시지 스케줄러 완료: successCount={}", successCount);
        } catch (Exception e) {
            log.error("격려 메시지 스케줄러 오류", e);
        }
    }
}
