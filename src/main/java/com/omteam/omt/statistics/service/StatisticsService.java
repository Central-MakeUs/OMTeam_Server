package com.omteam.omt.statistics.service;

import com.omteam.omt.common.util.DayOfWeekUtils;
import com.omteam.omt.common.util.MissionResultStats;
import com.omteam.omt.mission.domain.DailyMissionResult;
import com.omteam.omt.mission.domain.MissionResult;
import com.omteam.omt.mission.domain.MissionType;
import com.omteam.omt.mission.repository.DailyMissionResultRepository;
import com.omteam.omt.statistics.dto.MissionTypeStatisticsResponse;
import com.omteam.omt.statistics.dto.MissionTypeStatisticsResponse.TypeStatistics;
import com.omteam.omt.statistics.dto.MonthlyPatternResponse;
import com.omteam.omt.statistics.dto.MonthlyPatternResponse.AiFeedback;
import com.omteam.omt.statistics.dto.MonthlyPatternResponse.DayOfWeekStatistics;
import com.omteam.omt.statistics.dto.WeeklyStatisticsResponse;
import com.omteam.omt.statistics.dto.WeeklyStatisticsResponse.DailyResult;
import com.omteam.omt.statistics.dto.WeeklyStatisticsResponse.DailyStatus;
import com.omteam.omt.statistics.dto.WeeklyStatisticsResponse.WeeklySummary;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsService {

    private static final int DAYS_IN_WEEK = 7;
    private static final int MONTHLY_PATTERN_DAYS = 30;
    private static final double LOW_SUCCESS_RATE_THRESHOLD = 50.0;

    private final DailyMissionResultRepository missionResultRepository;

    /**
     * 주간 통계 조회 (이번주 + 지난주 비교)
     */
    public WeeklyStatisticsResponse getWeeklyStatistics(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate thisWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate thisWeekEnd = thisWeekStart.plusDays(DAYS_IN_WEEK - 1);
        LocalDate lastWeekStart = thisWeekStart.minusDays(DAYS_IN_WEEK);
        LocalDate lastWeekEnd = thisWeekStart.minusDays(1);

        List<DailyMissionResult> thisWeekResults = missionResultRepository
                .findByUserUserIdAndMissionDateBetweenOrderByMissionDateDesc(
                        userId, thisWeekStart, thisWeekEnd);

        List<DailyMissionResult> lastWeekResults = missionResultRepository
                .findByUserUserIdAndMissionDateBetweenOrderByMissionDateDesc(
                        userId, lastWeekStart, lastWeekEnd);

        List<DailyResult> dailyResults = buildDailyResults(thisWeekStart, today, thisWeekResults);

        return WeeklyStatisticsResponse.builder()
                .weekStartDate(thisWeekStart)
                .weekEndDate(thisWeekEnd)
                .thisWeek(buildWeeklySummary(thisWeekResults, thisWeekStart, today))
                .lastWeek(buildWeeklySummary(lastWeekResults, lastWeekStart, lastWeekEnd))
                .dailyResults(dailyResults)
                .build();
    }

    private WeeklySummary buildWeeklySummary(List<DailyMissionResult> results,
                                              LocalDate weekStart, LocalDate weekEnd) {
        LocalDate today = LocalDate.now();
        LocalDate effectiveEnd = weekEnd.isAfter(today) ? today : weekEnd;
        int daysElapsed = (int) (effectiveEnd.toEpochDay() - weekStart.toEpochDay()) + 1;

        MissionResultStats stats = MissionResultStats.fromWithDaysBase(results, daysElapsed);
        int notPerformedCount = Math.max(0, daysElapsed - results.size());

        return WeeklySummary.builder()
                .totalCount(stats.totalCount())
                .successCount(stats.successCount())
                .failureCount(stats.failureCount())
                .notPerformedCount(notPerformedCount)
                .successRate(stats.successRate())
                .build();
    }

    private List<DailyResult> buildDailyResults(LocalDate weekStart, LocalDate today,
                                                 List<DailyMissionResult> results) {
        Map<LocalDate, DailyMissionResult> resultMap = results.stream()
                .collect(Collectors.toMap(DailyMissionResult::getMissionDate, Function.identity()));

        List<DailyResult> dailyResults = new ArrayList<>();

        for (int i = 0; i < DAYS_IN_WEEK; i++) {
            LocalDate date = weekStart.plusDays(i);

            if (date.isAfter(today)) {
                break;
            }

            DailyResult dailyResult = buildDailyResult(date, resultMap.get(date));
            dailyResults.add(dailyResult);
        }

        return dailyResults;
    }

    private DailyResult buildDailyResult(LocalDate date, DailyMissionResult result) {
        if (result != null) {
            return DailyResult.builder()
                    .date(date)
                    .dayOfWeek(date.getDayOfWeek())
                    .status(result.getResult() == MissionResult.SUCCESS
                            ? DailyStatus.SUCCESS : DailyStatus.FAILURE)
                    .missionType(result.getMission().getType())
                    .missionTitle(result.getMission().getName())
                    .build();
        }

        return DailyResult.builder()
                .date(date)
                .dayOfWeek(date.getDayOfWeek())
                .status(DailyStatus.NOT_PERFORMED)
                .missionType(null)
                .missionTitle(null)
                .build();
    }

    /**
     * 미션 종류별 통계 조회
     */
    public MissionTypeStatisticsResponse getMissionTypeStatistics(Long userId) {
        List<DailyMissionResult> allResults = missionResultRepository
                .findByUserUserIdOrderByMissionDateDesc(userId);

        Map<MissionType, List<DailyMissionResult>> resultsByType = allResults.stream()
                .collect(Collectors.groupingBy(r -> r.getMission().getType()));

        List<TypeStatistics> typeStatistics = new ArrayList<>();
        int totalSuccessCount = 0;

        for (MissionType type : MissionType.values()) {
            List<DailyMissionResult> typeResults = resultsByType.getOrDefault(type, List.of());
            MissionResultStats stats = MissionResultStats.from(typeResults);

            totalSuccessCount += stats.successCount();

            typeStatistics.add(TypeStatistics.builder()
                    .type(type)
                    .typeName(type.getDisplayName())
                    .successCount(stats.successCount())
                    .failureCount(stats.failureCount())
                    .totalCount(stats.totalCount())
                    .successRate(stats.successRate())
                    .build());
        }

        return MissionTypeStatisticsResponse.builder()
                .totalSuccessCount(totalSuccessCount)
                .byType(typeStatistics)
                .build();
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

        List<DayOfWeekStatistics> dayOfWeekStats = buildDayOfWeekStatistics(resultsByDayOfWeek);
        AiFeedback aiFeedback = generatePatternFeedback(dayOfWeekStats);

        return MonthlyPatternResponse.builder()
                .startDate(monthAgo)
                .endDate(today)
                .dayOfWeekStats(dayOfWeekStats)
                .aiFeedback(aiFeedback)
                .build();
    }

    private List<DayOfWeekStatistics> buildDayOfWeekStatistics(
            Map<DayOfWeek, List<DailyMissionResult>> resultsByDayOfWeek) {
        List<DayOfWeekStatistics> stats = new ArrayList<>();

        for (DayOfWeek dow : DayOfWeek.values()) {
            List<DailyMissionResult> dowResults = resultsByDayOfWeek.getOrDefault(dow, List.of());
            MissionResultStats missionStats = MissionResultStats.from(dowResults);

            stats.add(DayOfWeekStatistics.builder()
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

    private AiFeedback generatePatternFeedback(List<DayOfWeekStatistics> stats) {
        DayOfWeekStatistics bestDay = findBestPerformingDay(stats);
        DayOfWeekStatistics worstDay = findWorstPerformingDay(stats);

        if (bestDay == null) {
            return buildInsufficientDataFeedback();
        }

        String summary = String.format("%s에 가장 잘 수행하고 있어요! (성공률 %.0f%%)",
                bestDay.dayName(), bestDay.successRate());

        String recommendation = buildRecommendation(bestDay, worstDay);

        return AiFeedback.builder()
                .summary(summary)
                .recommendation(recommendation)
                .build();
    }

    private DayOfWeekStatistics findBestPerformingDay(List<DayOfWeekStatistics> stats) {
        return stats.stream()
                .filter(s -> s.totalCount() > 0)
                .max((a, b) -> Double.compare(a.successRate(), b.successRate()))
                .orElse(null);
    }

    private DayOfWeekStatistics findWorstPerformingDay(List<DayOfWeekStatistics> stats) {
        return stats.stream()
                .filter(s -> s.totalCount() > 0)
                .min((a, b) -> Double.compare(a.successRate(), b.successRate()))
                .orElse(null);
    }

    private AiFeedback buildInsufficientDataFeedback() {
        return AiFeedback.builder()
                .summary("아직 충분한 데이터가 없습니다.")
                .recommendation("꾸준히 미션을 수행하면 더 정확한 분석을 받을 수 있어요!")
                .build();
    }

    private String buildRecommendation(DayOfWeekStatistics bestDay, DayOfWeekStatistics worstDay) {
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
