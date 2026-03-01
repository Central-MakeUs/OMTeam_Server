package com.omteam.omt.report.scheduler;

import com.omteam.omt.report.service.AnalysisRetryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisRetryScheduler {

    private final AnalysisRetryService analysisRetryService;

    /**
     * 정시마다 실패한 AI 분석을 재시도한다.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void retryPendingAnalyses() {
        log.info("분석 재시도 스케줄러 시작");
        analysisRetryService.processRetries();
    }
}
