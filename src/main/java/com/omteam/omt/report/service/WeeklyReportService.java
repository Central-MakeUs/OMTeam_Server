package com.omteam.omt.report.service;

import com.omteam.omt.mission.domain.MissionResult;
import com.omteam.omt.mission.repository.DailyMissionResultRepository;
import com.omteam.omt.report.domain.WeeklyAiAnalysis;
import com.omteam.omt.report.dto.WeeklyReportResponse;
import com.omteam.omt.report.dto.WeeklyReportResponse.AiAnalysis;
import com.omteam.omt.report.dto.WeeklyReportResponse.FailureReasonRank;
import com.omteam.omt.report.repository.WeeklyAiAnalysisRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WeeklyReportService {

    private static final int DAYS_IN_WEEK = 7;
    private static final int TOP_FAILURE_REASONS_LIMIT = 5;

    private final WeeklyAiAnalysisRepository weeklyAiAnalysisRepository;
    private final DailyMissionResultRepository missionResultRepository;

    /**
     * 이번주 또는 특정 주의 AI 분석 리포트 조회
     */
    public WeeklyReportResponse getWeeklyReport(Long userId, LocalDate weekStartDate) {
        LocalDate effectiveStartDate = resolveWeekStartDate(weekStartDate);
        LocalDate weekEndDate = effectiveStartDate.plusDays(DAYS_IN_WEEK - 1);

        AiAnalysis aiAnalysis = getAiAnalysis(userId, effectiveStartDate);
        List<FailureReasonRank> failureReasons = getFailureReasonRanks(userId, effectiveStartDate, weekEndDate);

        return WeeklyReportResponse.builder()
                .weekStartDate(effectiveStartDate)
                .weekEndDate(weekEndDate)
                .aiAnalysis(aiAnalysis)
                .topFailureReasons(failureReasons)
                .build();
    }

    private LocalDate resolveWeekStartDate(LocalDate weekStartDate) {
        if (weekStartDate != null) {
            return weekStartDate;
        }
        return LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private AiAnalysis getAiAnalysis(Long userId, LocalDate weekStartDate) {
        return weeklyAiAnalysisRepository
                .findByUserUserIdAndWeekStartDate(userId, weekStartDate)
                .map(this::toAiAnalysis)
                .orElseGet(this::buildDefaultAiAnalysis);
    }

    private AiAnalysis toAiAnalysis(WeeklyAiAnalysis analysis) {
        return AiAnalysis.builder()
                .summary(analysis.getSummary())
                .insight(analysis.getInsight())
                .recommendation(analysis.getRecommendation())
                .build();
    }

    private AiAnalysis buildDefaultAiAnalysis() {
        return AiAnalysis.builder()
                .summary("아직 이번 주 AI 분석 결과가 없습니다.")
                .insight("매주 월요일에 지난 주 분석 결과가 생성됩니다.")
                .recommendation("꾸준히 미션을 수행해주세요!")
                .build();
    }

    private List<FailureReasonRank> getFailureReasonRanks(Long userId, LocalDate startDate, LocalDate endDate) {
        List<String> reasons = missionResultRepository
                .findFailureReasonsByUserIdAndDateRange(userId, MissionResult.FAILURE, startDate, endDate);

        Map<String, Long> reasonCounts = reasons.stream()
                .filter(reason -> reason != null && !reason.isBlank())
                .collect(Collectors.groupingBy(reason -> reason, Collectors.counting()));

        AtomicInteger rankCounter = new AtomicInteger(1);

        return reasonCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(TOP_FAILURE_REASONS_LIMIT)
                .map(entry -> FailureReasonRank.builder()
                        .rank(rankCounter.getAndIncrement())
                        .reason(entry.getKey())
                        .count(entry.getValue().intValue())
                        .build())
                .toList();
    }
}
