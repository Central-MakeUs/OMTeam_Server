package com.omteam.omt.report.scheduler;

import com.omteam.omt.report.dto.BatchProcessResult;
import com.omteam.omt.report.service.AnalysisRetryService;
import com.omteam.omt.report.service.WeeklyAnalysisService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyAnalysisScheduler {

    private final WeeklyAnalysisService weeklyAnalysisService;
    private final AnalysisRetryService analysisRetryService;

    /**
     * 매주 월요일 새벽 1시에 지난주 분석 결과 생성
     */
    @Scheduled(cron = "0 0 1 * * MON")
    public void generateWeeklyAnalysis() {
        log.info("주간 AI 분석 스케줄러 시작");

        LocalDate lastWeekMonday = LocalDate.now()
                .minusWeeks(1)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        try {
            BatchProcessResult result = weeklyAnalysisService.generateWeeklyAnalysisForAllUsers(lastWeekMonday);
            log.info("주간 AI 분석 스케줄러 종료: total={}, success={}, failed={}",
                    result.totalCount(), result.successCount(), result.failedCount());

            result.failedUserIds().forEach(userId ->
                    analysisRetryService.enqueue(userId, "WEEKLY", lastWeekMonday));
        } catch (Exception e) {
            log.error("주간 AI 분석 스케줄러 오류", e);
        }
    }
}
