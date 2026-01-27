package com.omteam.omt.report.service;

import com.omteam.omt.common.util.DayOfWeekUtils;
import com.omteam.omt.common.util.MissionResultStats;
import com.omteam.omt.mission.domain.DailyMissionResult;
import com.omteam.omt.mission.domain.MissionResult;
import com.omteam.omt.mission.domain.MissionType;
import com.omteam.omt.mission.repository.DailyMissionResultRepository;
import com.omteam.omt.report.domain.WeeklyAiAnalysis;
import com.omteam.omt.report.dto.MonthlyPatternResponse;
import com.omteam.omt.report.dto.WeeklyReportResponse;
import com.omteam.omt.report.dto.WeeklyReportResponse.AiFeedback;
import com.omteam.omt.report.dto.WeeklyReportResponse.DailyResult;
import com.omteam.omt.report.dto.WeeklyReportResponse.DailyStatus;
import com.omteam.omt.report.dto.WeeklyReportResponse.FailureReasonRank;
import com.omteam.omt.report.dto.WeeklyReportResponse.TypeSuccessCount;
import com.omteam.omt.report.repository.WeeklyAiAnalysisRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WeeklyReportService {

    private static final int MONTHLY_PATTERN_DAYS = 30;
    private static final double LOW_SUCCESS_RATE_THRESHOLD = 50.0;

    private final DailyMissionResultRepository missionResultRepository;
    private final WeeklyAiAnalysisRepository weeklyAiAnalysisRepository;

    public WeeklyReportResponse getWeeklyReport(Long userId, LocalDate weekStartDate) {
        LocalDate effectiveStart = resolveWeekStartDate(weekStartDate);
        LocalDate weekEnd = effectiveStart.plusDays(6);
        LocalDate today = LocalDate.now();

        // 미션 결과 조회
        List<DailyMissionResult> thisWeekResults = missionResultRepository
                .findByUserUserIdAndMissionDateBetween(userId, effectiveStart, weekEnd);

        LocalDate lastWeekStart = effectiveStart.minusDays(7);
        LocalDate lastWeekEnd = effectiveStart.minusDays(1);
        List<DailyMissionResult> lastWeekResults = missionResultRepository
                .findByUserUserIdAndMissionDateBetween(userId, lastWeekStart, lastWeekEnd);

        // 1. 성공률 계산
        double thisWeekRate = calculateSuccessRate(thisWeekResults, effectiveStart, today, weekEnd);
        double lastWeekRate = calculateSuccessRate(lastWeekResults, lastWeekStart, lastWeekEnd, lastWeekEnd);

        // 2. 요일별 결과
        List<DailyResult> dailyResults = buildDailyResults(effectiveStart, today, weekEnd, thisWeekResults);

        // 3. 타입별 성공횟수
        List<TypeSuccessCount> typeCounts = calculateTypeSuccessCounts(thisWeekResults);

        // 4. 실패 원인 순위
        List<FailureReasonRank> failureRanks = getFailureReasonRanks(thisWeekResults);

        // 5. AI 피드백 (DB에서 조회)
        AiFeedback feedback = getAiFeedback(userId, effectiveStart);

        return WeeklyReportResponse.builder()
                .weekStartDate(effectiveStart)
                .weekEndDate(weekEnd)
                .thisWeekSuccessRate(thisWeekRate)
                .lastWeekSuccessRate(lastWeekRate)
                .dailyResults(dailyResults)
                .typeSuccessCounts(typeCounts)
                .topFailureReasons(failureRanks)
                .aiFeedback(feedback)
                .build();
    }

    private LocalDate resolveWeekStartDate(LocalDate weekStartDate) {
        if (weekStartDate == null) {
            return LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        }
        return weekStartDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private double calculateSuccessRate(List<DailyMissionResult> results,
                                        LocalDate startDate, LocalDate endDate, LocalDate weekEnd) {
        LocalDate effectiveEnd = endDate.isBefore(weekEnd) ? endDate : weekEnd;
        long totalDays = startDate.datesUntil(effectiveEnd.plusDays(1)).count();

        if (totalDays == 0) {
            return 0.0;
        }

        long successCount = results.stream()
                .filter(r -> r.getResult() == MissionResult.SUCCESS)
                .filter(r -> !r.getMissionDate().isAfter(effectiveEnd))
                .count();

        return Math.round((double) successCount / totalDays * 1000) / 10.0;
    }

    private List<DailyResult> buildDailyResults(LocalDate startDate, LocalDate today,
                                                 LocalDate weekEnd, List<DailyMissionResult> results) {
        Map<LocalDate, DailyMissionResult> resultMap = results.stream()
                .collect(Collectors.toMap(DailyMissionResult::getMissionDate, r -> r, (a, b) -> a));

        List<DailyResult> dailyResults = new ArrayList<>();
        LocalDate effectiveEnd = today.isBefore(weekEnd) ? today : weekEnd;

        for (LocalDate date = startDate; !date.isAfter(effectiveEnd); date = date.plusDays(1)) {
            DailyMissionResult result = resultMap.get(date);

            DailyResult dailyResult;
            if (result != null) {
                DailyStatus status = result.getResult() == MissionResult.SUCCESS
                        ? DailyStatus.SUCCESS : DailyStatus.FAILURE;
                dailyResult = DailyResult.builder()
                        .date(date)
                        .dayOfWeek(date.getDayOfWeek())
                        .status(status)
                        .missionType(result.getMission().getType())
                        .missionTitle(result.getMission().getName())
                        .build();
            } else {
                dailyResult = DailyResult.builder()
                        .date(date)
                        .dayOfWeek(date.getDayOfWeek())
                        .status(DailyStatus.NOT_PERFORMED)
                        .missionType(null)
                        .missionTitle(null)
                        .build();
            }
            dailyResults.add(dailyResult);
        }

        return dailyResults;
    }

    private List<TypeSuccessCount> calculateTypeSuccessCounts(List<DailyMissionResult> results) {
        Map<MissionType, Long> successByType = results.stream()
                .filter(r -> r.getResult() == MissionResult.SUCCESS)
                .collect(Collectors.groupingBy(
                        r -> r.getMission().getType(),
                        Collectors.counting()
                ));

        List<TypeSuccessCount> typeCounts = new ArrayList<>();

        typeCounts.add(TypeSuccessCount.builder()
                .type(MissionType.EXERCISE)
                .typeName("운동")
                .successCount(successByType.getOrDefault(MissionType.EXERCISE, 0L).intValue())
                .build());

        typeCounts.add(TypeSuccessCount.builder()
                .type(MissionType.DIET)
                .typeName("식단")
                .successCount(successByType.getOrDefault(MissionType.DIET, 0L).intValue())
                .build());

        return typeCounts;
    }

    private List<FailureReasonRank> getFailureReasonRanks(List<DailyMissionResult> results) {
        Map<String, Long> reasonCounts = results.stream()
                .filter(r -> r.getResult() == MissionResult.FAILURE)
                .map(DailyMissionResult::getFailureReason)
                .filter(reason -> reason != null && !reason.isBlank())
                .collect(Collectors.groupingBy(reason -> reason, Collectors.counting()));

        List<Map.Entry<String, Long>> sortedReasons = reasonCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .toList();

        List<FailureReasonRank> ranks = new ArrayList<>();
        for (int i = 0; i < sortedReasons.size(); i++) {
            Map.Entry<String, Long> entry = sortedReasons.get(i);
            ranks.add(FailureReasonRank.builder()
                    .rank(i + 1)
                    .reason(entry.getKey())
                    .count(entry.getValue().intValue())
                    .build());
        }

        return ranks;
    }

    private AiFeedback getAiFeedback(Long userId, LocalDate weekStartDate) {
        Optional<WeeklyAiAnalysis> analysis = weeklyAiAnalysisRepository
                .findByUserUserIdAndWeekStartDate(userId, weekStartDate);

        return analysis
                .map(a -> AiFeedback.builder()
                        .mainFailureReason(a.getMainFailureReason())
                        .overallFeedback(a.getOverallFeedback())
                        .build())
                .orElse(AiFeedback.builder()
                        .mainFailureReason(null)
                        .overallFeedback("아직 AI 분석 결과가 생성되지 않았습니다.")
                        .build());
    }

    /**
     * 월간 요일별 패턴 분석
     */
    public MonthlyPatternResponse getMonthlyPattern(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate monthAgo = today.minusDays(MONTHLY_PATTERN_DAYS);

        List<DailyMissionResult> results = missionResultRepository
                .findByUserUserIdAndMissionDateBetweenOrderByMissionDateDesc(userId, monthAgo, today);

        Map<DayOfWeek, List<DailyMissionResult>> resultsByDayOfWeek = results.stream()
                .collect(Collectors.groupingBy(r -> r.getMissionDate().getDayOfWeek()));

        List<MonthlyPatternResponse.DayOfWeekStatistics> dayOfWeekStats =
                buildDayOfWeekStatistics(resultsByDayOfWeek);
        MonthlyPatternResponse.AiFeedback aiFeedback = generatePatternFeedback(dayOfWeekStats);

        return MonthlyPatternResponse.builder()
                .startDate(monthAgo)
                .endDate(today)
                .dayOfWeekStats(dayOfWeekStats)
                .aiFeedback(aiFeedback)
                .build();
    }

    private List<MonthlyPatternResponse.DayOfWeekStatistics> buildDayOfWeekStatistics(
            Map<DayOfWeek, List<DailyMissionResult>> resultsByDayOfWeek) {
        List<MonthlyPatternResponse.DayOfWeekStatistics> stats = new ArrayList<>();

        for (DayOfWeek dow : DayOfWeek.values()) {
            List<DailyMissionResult> dowResults = resultsByDayOfWeek.getOrDefault(dow, List.of());
            MissionResultStats missionStats = MissionResultStats.from(dowResults);

            stats.add(MonthlyPatternResponse.DayOfWeekStatistics.builder()
                    .dayOfWeek(dow)
                    .dayName(DayOfWeekUtils.toKorean(dow))
                    .totalCount(missionStats.totalCount())
                    .successCount(missionStats.successCount())
                    .failureCount(missionStats.failureCount())
                    .successRate(missionStats.successRate())
                    .build());
        }

        return stats;
    }

    private MonthlyPatternResponse.AiFeedback generatePatternFeedback(
            List<MonthlyPatternResponse.DayOfWeekStatistics> stats) {
        MonthlyPatternResponse.DayOfWeekStatistics bestDay = findBestPerformingDay(stats);
        MonthlyPatternResponse.DayOfWeekStatistics worstDay = findWorstPerformingDay(stats);

        if (bestDay == null) {
            return buildInsufficientDataFeedback();
        }

        String summary = String.format("%s에 가장 잘 수행하고 있어요! (성공률 %.0f%%)",
                bestDay.dayName(), bestDay.successRate());

        String recommendation = buildRecommendation(bestDay, worstDay);

        return MonthlyPatternResponse.AiFeedback.builder()
                .summary(summary)
                .recommendation(recommendation)
                .build();
    }

    private MonthlyPatternResponse.DayOfWeekStatistics findBestPerformingDay(
            List<MonthlyPatternResponse.DayOfWeekStatistics> stats) {
        return stats.stream()
                .filter(s -> s.totalCount() > 0)
                .max((a, b) -> Double.compare(a.successRate(), b.successRate()))
                .orElse(null);
    }

    private MonthlyPatternResponse.DayOfWeekStatistics findWorstPerformingDay(
            List<MonthlyPatternResponse.DayOfWeekStatistics> stats) {
        return stats.stream()
                .filter(s -> s.totalCount() > 0)
                .min((a, b) -> Double.compare(a.successRate(), b.successRate()))
                .orElse(null);
    }

    private MonthlyPatternResponse.AiFeedback buildInsufficientDataFeedback() {
        return MonthlyPatternResponse.AiFeedback.builder()
                .summary("아직 충분한 데이터가 없습니다.")
                .recommendation("꾸준히 미션을 수행하면 더 정확한 분석을 받을 수 있어요!")
                .build();
    }

    private String buildRecommendation(MonthlyPatternResponse.DayOfWeekStatistics bestDay,
                                        MonthlyPatternResponse.DayOfWeekStatistics worstDay) {
        boolean hasLowPerformingDay = worstDay != null
                && worstDay.successRate() < LOW_SUCCESS_RATE_THRESHOLD
                && !worstDay.dayOfWeek().equals(bestDay.dayOfWeek());

        if (hasLowPerformingDay) {
            return String.format("%s은 휴식하고, %s에 가벼운 운동을 시도해보세요.",
                    worstDay.dayName(), bestDay.dayName());
        }

        return "현재 패턴을 유지하면서 꾸준히 진행해보세요!";
    }
}
