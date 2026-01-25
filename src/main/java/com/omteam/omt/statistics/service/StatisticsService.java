package com.omteam.omt.statistics.service;

import com.omteam.omt.mission.domain.DailyMissionResult;
import com.omteam.omt.mission.domain.MissionResult;
import com.omteam.omt.mission.domain.MissionType;
import com.omteam.omt.mission.repository.DailyMissionResultRepository;
import com.omteam.omt.statistics.dto.MissionTypeStatisticsResponse;
import com.omteam.omt.statistics.dto.MissionTypeStatisticsResponse.TypeStatistics;
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
}
