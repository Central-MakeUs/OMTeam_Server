package com.omteam.omt.report.service;

import com.omteam.omt.mission.domain.DailyMissionResult;
import com.omteam.omt.mission.domain.MissionResult;
import com.omteam.omt.mission.domain.MissionType;
import com.omteam.omt.mission.repository.DailyMissionResultRepository;
import com.omteam.omt.report.domain.WeeklyAiAnalysis;
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
}
