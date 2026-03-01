package com.omteam.omt.report.service;

import com.omteam.omt.report.domain.AnalysisRetryQueue;
import com.omteam.omt.report.repository.AnalysisRetryQueueRepository;
import com.omteam.omt.user.domain.User;
import com.omteam.omt.user.service.UserQueryService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisRetryService {

    private static final int MAX_RETRY_COUNT = 5;

    private final AnalysisRetryQueueRepository retryQueueRepository;
    private final DailyAnalysisService dailyAnalysisService;
    private final WeeklyAnalysisService weeklyAnalysisService;
    private final UserQueryService userQueryService;

    @Transactional
    public void enqueue(Long userId, String analysisType, LocalDate targetDate) {
        retryQueueRepository.findByUserIdAndAnalysisTypeAndTargetDate(userId, analysisType, targetDate)
                .ifPresentOrElse(
                        existing -> log.debug("이미 재시도 큐에 존재: userId={}, type={}, date={}",
                                userId, analysisType, targetDate),
                        () -> retryQueueRepository.save(
                                AnalysisRetryQueue.create(userId, analysisType, targetDate))
                );
    }

    @Transactional
    public void processRetries() {
        List<AnalysisRetryQueue> pending =
                retryQueueRepository.findByStatus(AnalysisRetryQueue.STATUS_PENDING);
        log.info("재시도 큐 처리 시작: {} 건", pending.size());

        for (AnalysisRetryQueue item : pending) {
            try {
                User user = userQueryService.getUser(item.getUserId());

                if ("DAILY".equals(item.getAnalysisType())) {
                    dailyAnalysisService.generateDailyAnalysisForUser(user, item.getTargetDate());
                } else {
                    LocalDate weekEnd = item.getTargetDate().plusDays(6);
                    weeklyAnalysisService.generateWeeklyAnalysisForUser(
                            user, item.getTargetDate(), weekEnd);
                }

                retryQueueRepository.delete(item);
                log.info("재시도 성공: userId={}, type={}, date={}",
                        item.getUserId(), item.getAnalysisType(), item.getTargetDate());

            } catch (Exception e) {
                item.incrementRetry(LocalDateTime.now());
                if (item.getRetryCount() >= MAX_RETRY_COUNT) {
                    item.markExceeded();
                    log.warn("재시도 횟수 초과 (포기): userId={}, type={}, date={}",
                            item.getUserId(), item.getAnalysisType(), item.getTargetDate());
                } else {
                    log.warn("재시도 실패 ({}/{}): userId={}, error={}",
                            item.getRetryCount(), MAX_RETRY_COUNT,
                            item.getUserId(), e.getMessage());
                }
                retryQueueRepository.save(item);
            }
        }
    }
}
