package com.omteam.omt.statistics.service;

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

    private final DailyMissionResultRepository missionResultRepository;

    /**
     * 주간 통계 조회 (이번주 + 지난주 비교)
     */
    public WeeklyStatisticsResponse getWeeklyStatistics(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate thisWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate thisWeekEnd = thisWeekStart.plusDays(6);
        LocalDate lastWeekStart = thisWeekStart.minusDays(7);
        LocalDate lastWeekEnd = thisWeekStart.minusDays(1);

        // 이번주 데이터 조회
        List<DailyMissionResult> thisWeekResults = missionResultRepository
                .findByUserUserIdAndMissionDateBetweenOrderByMissionDateDesc(
                        userId, thisWeekStart, thisWeekEnd);

        // 지난주 데이터 조회
        List<DailyMissionResult> lastWeekResults = missionResultRepository
                .findByUserUserIdAndMissionDateBetweenOrderByMissionDateDesc(
                        userId, lastWeekStart, lastWeekEnd);

        // 이번주 요일별 결과 생성
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
        int successCount = (int) results.stream()
                .filter(r -> r.getResult() == MissionResult.SUCCESS)
                .count();
        int failureCount = (int) results.stream()
                .filter(r -> r.getResult() == MissionResult.FAILURE)
                .count();

        // 경과일 계산 (오늘까지만)
        LocalDate today = LocalDate.now();
        LocalDate effectiveEnd = weekEnd.isAfter(today) ? today : weekEnd;
        int daysElapsed = (int) (effectiveEnd.toEpochDay() - weekStart.toEpochDay()) + 1;
        int notPerformedCount = Math.max(0, daysElapsed - results.size());

        int totalCount = successCount + failureCount;
        double successRate = daysElapsed > 0 ? (successCount * 100.0 / daysElapsed) : 0;

        return WeeklySummary.builder()
                .totalCount(totalCount)
                .successCount(successCount)
                .failureCount(failureCount)
                .notPerformedCount(notPerformedCount)
                .successRate(Math.round(successRate * 10) / 10.0)
                .build();
    }

    private List<DailyResult> buildDailyResults(LocalDate weekStart, LocalDate today,
                                                 List<DailyMissionResult> results) {
        Map<LocalDate, DailyMissionResult> resultMap = results.stream()
                .collect(Collectors.toMap(DailyMissionResult::getMissionDate, Function.identity()));

        List<DailyResult> dailyResults = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            LocalDate date = weekStart.plusDays(i);

            // 미래 날짜는 제외
            if (date.isAfter(today)) {
                break;
            }

            DailyMissionResult result = resultMap.get(date);

            DailyResult dailyResult;
            if (result != null) {
                dailyResult = DailyResult.builder()
                        .date(date)
                        .dayOfWeek(date.getDayOfWeek())
                        .status(result.getResult() == MissionResult.SUCCESS
                                ? DailyStatus.SUCCESS : DailyStatus.FAILURE)
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

    /**
     * 미션 종류별 통계 조회
     */
    public MissionTypeStatisticsResponse getMissionTypeStatistics(Long userId) {
        List<DailyMissionResult> allResults = missionResultRepository.findByUserUserIdOrderByMissionDateDesc(userId);

        Map<MissionType, List<DailyMissionResult>> resultsByType = allResults.stream()
                .collect(Collectors.groupingBy(r -> r.getMission().getType()));

        List<TypeStatistics> typeStatistics = new ArrayList<>();
        int totalSuccessCount = 0;

        for (MissionType type : MissionType.values()) {
            List<DailyMissionResult> typeResults = resultsByType.getOrDefault(type, List.of());

            int successCount = (int) typeResults.stream()
                    .filter(r -> r.getResult() == MissionResult.SUCCESS)
                    .count();
            int failureCount = (int) typeResults.stream()
                    .filter(r -> r.getResult() == MissionResult.FAILURE)
                    .count();
            int totalCount = successCount + failureCount;
            double successRate = totalCount > 0 ? (successCount * 100.0 / totalCount) : 0;

            totalSuccessCount += successCount;

            typeStatistics.add(TypeStatistics.builder()
                    .type(type)
                    .typeName(getTypeName(type))
                    .successCount(successCount)
                    .failureCount(failureCount)
                    .totalCount(totalCount)
                    .successRate(Math.round(successRate * 10) / 10.0)
                    .build());
        }

        return MissionTypeStatisticsResponse.builder()
                .totalSuccessCount(totalSuccessCount)
                .byType(typeStatistics)
                .build();
    }

    private String getTypeName(MissionType type) {
        return switch (type) {
            case EXERCISE -> "운동";
            case DIET -> "식단";
        };
    }

    /**
     * 월간 요일별 패턴 분석
     */
    public MonthlyPatternResponse getMonthlyPattern(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate monthAgo = today.minusDays(30);

        List<DailyMissionResult> results = missionResultRepository
                .findByUserUserIdAndMissionDateBetweenOrderByMissionDateDesc(userId, monthAgo, today);

        // 요일별 그룹핑
        Map<DayOfWeek, List<DailyMissionResult>> resultsByDayOfWeek = results.stream()
                .collect(Collectors.groupingBy(r -> r.getMissionDate().getDayOfWeek()));

        List<DayOfWeekStatistics> dayOfWeekStats = new ArrayList<>();

        for (DayOfWeek dow : DayOfWeek.values()) {
            List<DailyMissionResult> dowResults = resultsByDayOfWeek.getOrDefault(dow, List.of());

            int successCount = (int) dowResults.stream()
                    .filter(r -> r.getResult() == MissionResult.SUCCESS)
                    .count();
            int failureCount = (int) dowResults.stream()
                    .filter(r -> r.getResult() == MissionResult.FAILURE)
                    .count();
            int totalCount = successCount + failureCount;
            double successRate = totalCount > 0 ? (successCount * 100.0 / totalCount) : 0;

            dayOfWeekStats.add(DayOfWeekStatistics.builder()
                    .dayOfWeek(dow)
                    .dayName(getDayName(dow))
                    .totalCount(totalCount)
                    .successCount(successCount)
                    .failureCount(failureCount)
                    .successRate(Math.round(successRate * 10) / 10.0)
                    .build());
        }

        // AI 피드백 생성 (간단한 규칙 기반, 추후 AI 서버 연동으로 대체 가능)
        AiFeedback aiFeedback = generatePatternFeedback(dayOfWeekStats);

        return MonthlyPatternResponse.builder()
                .startDate(monthAgo)
                .endDate(today)
                .dayOfWeekStats(dayOfWeekStats)
                .aiFeedback(aiFeedback)
                .build();
    }

    private AiFeedback generatePatternFeedback(List<DayOfWeekStatistics> stats) {
        // 가장 성공률이 높은 요일과 낮은 요일 찾기
        DayOfWeekStatistics bestDay = stats.stream()
                .filter(s -> s.totalCount() > 0)
                .max((a, b) -> Double.compare(a.successRate(), b.successRate()))
                .orElse(null);

        DayOfWeekStatistics worstDay = stats.stream()
                .filter(s -> s.totalCount() > 0)
                .min((a, b) -> Double.compare(a.successRate(), b.successRate()))
                .orElse(null);

        if (bestDay == null) {
            return AiFeedback.builder()
                    .summary("아직 충분한 데이터가 없습니다.")
                    .recommendation("꾸준히 미션을 수행하면 더 정확한 분석을 받을 수 있어요!")
                    .build();
        }

        String summary = String.format("%s에 가장 잘 수행하고 있어요! (성공률 %.0f%%)",
                bestDay.dayName(), bestDay.successRate());

        String recommendation;
        if (worstDay != null && worstDay.successRate() < 50 && !worstDay.dayOfWeek().equals(bestDay.dayOfWeek())) {
            recommendation = String.format("%s은 휴식하고, %s에 가벼운 운동을 시도해보세요.",
                    worstDay.dayName(), bestDay.dayName());
        } else {
            recommendation = "현재 패턴을 유지하면서 꾸준히 진행해보세요!";
        }

        return AiFeedback.builder()
                .summary(summary)
                .recommendation(recommendation)
                .build();
    }

    private String getDayName(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "월요일";
            case TUESDAY -> "화요일";
            case WEDNESDAY -> "수요일";
            case THURSDAY -> "목요일";
            case FRIDAY -> "금요일";
            case SATURDAY -> "토요일";
            case SUNDAY -> "일요일";
        };
    }
}
