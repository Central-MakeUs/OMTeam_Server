package com.omteam.omt.report.service;

import com.omteam.omt.common.util.DayOfWeekUtils;
import com.omteam.omt.common.util.MissionResultStats;
import com.omteam.omt.mission.domain.DailyMissionResult;
import com.omteam.omt.mission.repository.DailyMissionResultRepository;
import com.omteam.omt.report.dto.MonthlyPatternResponse;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MonthlyPatternService {

    private static final int MONTHLY_PATTERN_DAYS = 30;
    private static final double LOW_SUCCESS_RATE_THRESHOLD = 50.0;

    private final DailyMissionResultRepository missionResultRepository;

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
        return java.util.Arrays.stream(DayOfWeek.values())
                .map(dow -> {
                    List<DailyMissionResult> dowResults = resultsByDayOfWeek.getOrDefault(dow, List.of());
                    MissionResultStats missionStats = MissionResultStats.from(dowResults);
                    return MonthlyPatternResponse.DayOfWeekStatistics.builder()
                            .dayOfWeek(dow)
                            .dayName(DayOfWeekUtils.toKorean(dow))
                            .totalCount(missionStats.totalCount())
                            .successCount(missionStats.successCount())
                            .failureCount(missionStats.failureCount())
                            .successRate(missionStats.successRate())
                            .build();
                })
                .toList();
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
