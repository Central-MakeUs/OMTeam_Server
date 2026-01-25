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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WeeklyReportService {

    private final WeeklyAiAnalysisRepository weeklyAiAnalysisRepository;
    private final DailyMissionResultRepository missionResultRepository;

    /**
     * 이번주 또는 특정 주의 AI 분석 리포트 조회
     */
    public WeeklyReportResponse getWeeklyReport(Long userId, LocalDate weekStartDate) {
        // 주 시작일이 null이면 이번주로 설정
        LocalDate effectiveStartDate = weekStartDate != null
                ? weekStartDate
                : LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEndDate = effectiveStartDate.plusDays(6);

        // AI 분석 결과 조회
        WeeklyAiAnalysis analysis = weeklyAiAnalysisRepository
                .findByUserUserIdAndWeekStartDate(userId, effectiveStartDate)
                .orElse(null);

        // 실패 원인 순위 계산
        List<FailureReasonRank> failureReasons = getFailureReasonRanks(userId, effectiveStartDate, weekEndDate);

        AiAnalysis aiAnalysis;
        if (analysis != null) {
            aiAnalysis = AiAnalysis.builder()
                    .summary(analysis.getSummary())
                    .insight(analysis.getInsight())
                    .recommendation(analysis.getRecommendation())
                    .build();
        } else {
            // AI 분석 결과가 없으면 기본 메시지
            aiAnalysis = AiAnalysis.builder()
                    .summary("아직 이번 주 AI 분석 결과가 없습니다.")
                    .insight("매주 월요일에 지난 주 분석 결과가 생성됩니다.")
                    .recommendation("꾸준히 미션을 수행해주세요!")
                    .build();
        }

        return WeeklyReportResponse.builder()
                .weekStartDate(effectiveStartDate)
                .weekEndDate(weekEndDate)
                .aiAnalysis(aiAnalysis)
                .topFailureReasons(failureReasons)
                .build();
    }

    private List<FailureReasonRank> getFailureReasonRanks(Long userId, LocalDate startDate, LocalDate endDate) {
        List<String> reasons = missionResultRepository
                .findFailureReasonsByUserIdAndDateRange(userId, MissionResult.FAILURE, startDate);

        // endDate까지만 필터링
        // (Repository 쿼리가 startDate 이후만 필터링하므로 추가 필터링 필요시 여기서 처리)

        // 실패 원인별 카운트
        Map<String, Long> reasonCounts = reasons.stream()
                .filter(r -> r != null && !r.isBlank())
                .collect(Collectors.groupingBy(r -> r, Collectors.counting()));

        // 순위별 정렬
        List<FailureReasonRank> ranks = new ArrayList<>();
        List<Map.Entry<String, Long>> sorted = reasonCounts.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(5)
                .toList();

        int rank = 1;
        for (Map.Entry<String, Long> entry : sorted) {
            ranks.add(FailureReasonRank.builder()
                    .rank(rank++)
                    .reason(entry.getKey())
                    .count(entry.getValue().intValue())
                    .build());
        }

        return ranks;
    }
}
