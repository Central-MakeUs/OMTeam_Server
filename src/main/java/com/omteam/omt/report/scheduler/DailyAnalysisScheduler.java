package com.omteam.omt.report.scheduler;

import com.omteam.omt.report.dto.BatchProcessResult;
import com.omteam.omt.report.service.DailyAnalysisService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyAnalysisScheduler {

    private final DailyAnalysisService dailyAnalysisService;

    /**
     * 매일 00:30에 오늘의 데일리 분석 결과를 생성한다.
     * (00:00에 미션 만료 스케줄러가 돌기 때문에 30분 후에 실행)
     */
    @Scheduled(cron = "0 30 0 * * *")
    public void generateDailyEncouragement() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("데일리 분석 스케줄러 시작: targetDate={}", yesterday);

        try {
            BatchProcessResult result = dailyAnalysisService.generateDailyEncouragementForAllUsers(yesterday);
            log.info("데일리 분석 스케줄러 완료: total={}, success={}, failed={}",
                    result.totalCount(), result.successCount(), result.failedCount());
        } catch (Exception e) {
            log.error("데일리 분석 스케줄러 오류", e);
        }
    }
}
