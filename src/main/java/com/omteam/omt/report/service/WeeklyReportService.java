package com.omteam.omt.report.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omteam.omt.common.exception.BusinessException;
import com.omteam.omt.common.exception.ErrorCode;
import com.omteam.omt.mission.domain.DailyMissionResult;
import com.omteam.omt.mission.domain.MissionResult;
import com.omteam.omt.mission.domain.MissionType;
import com.omteam.omt.mission.repository.DailyMissionResultRepository;
import com.omteam.omt.report.domain.WeeklyAiAnalysis;
import com.omteam.omt.report.dto.WeeklyReportResponse;
import com.omteam.omt.report.dto.WeeklyReportResponse.AiFeedback;
import com.omteam.omt.report.dto.WeeklyReportResponse.AiFailureReasonRank;
import com.omteam.omt.report.dto.WeeklyReportResponse.DailyResult;
import com.omteam.omt.report.dto.WeeklyReportResponse.DailyStatus;
import com.omteam.omt.report.dto.WeeklyReportResponse.FailureReasonRank;
import com.omteam.omt.report.dto.WeeklyReportResponse.TypeSuccessCount;
import com.omteam.omt.report.repository.WeeklyAiAnalysisRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WeeklyReportService {

    private final DailyMissionResultRepository missionResultRepository;
    private final WeeklyAiAnalysisRepository weeklyAiAnalysisRepository;
    private final ObjectMapper objectMapper;

    public WeeklyReportResponse getWeeklyReport(Long userId, Integer year, Integer month, Integer weekOfMonth) {
        LocalDate effectiveStart = resolveWeekStartDate(year, month, weekOfMonth);
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

        // 2. 성공 횟수 계산
        long successCount = calculateSuccessCount(thisWeekResults, today, weekEnd);

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
                .thisWeekSuccessCount(successCount)
                .dailyResults(dailyResults)
                .typeSuccessCounts(typeCounts)
                .topFailureReasons(failureRanks)
                .aiFeedback(feedback)
                .build();
    }

    private LocalDate resolveWeekStartDate(Integer year, Integer month, Integer weekOfMonth) {
        if (year == null || month == null || weekOfMonth == null) {
            return LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        }

        if (weekOfMonth < 1) {
            throw new BusinessException(ErrorCode.INVALID_WEEK_OF_MONTH);
        }

        // 해당 월의 첫 번째 월요일 계산
        LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);
        LocalDate firstMonday = firstDayOfMonth.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));

        // 해당 월의 마지막 날
        LocalDate lastDayOfMonth = firstDayOfMonth.with(TemporalAdjusters.lastDayOfMonth());

        // 요청한 주차의 월요일 계산
        LocalDate targetMonday = firstMonday.plusDays((long) (weekOfMonth - 1) * 7);

        // 해당 월요일이 해당 월을 벗어나면 예외
        if (targetMonday.isAfter(lastDayOfMonth)) {
            throw new BusinessException(ErrorCode.INVALID_WEEK_OF_MONTH);
        }

        return targetMonday;
    }

    private double calculateSuccessRate(List<DailyMissionResult> results,
                                        LocalDate startDate, LocalDate endDate, LocalDate weekEnd) {
        LocalDate effectiveEnd = endDate.isBefore(weekEnd) ? endDate : weekEnd;
        long totalDays = startDate.datesUntil(effectiveEnd.plusDays(1)).count();

        if (totalDays == 0) {
            return 0.0;
        }

        long successCount = calculateSuccessCount(results, endDate, weekEnd);

        return Math.round((double) successCount / totalDays * 1000) / 10.0;
    }

    private long calculateSuccessCount(List<DailyMissionResult> results, LocalDate endDate, LocalDate weekEnd) {
        LocalDate effectiveEnd = endDate.isBefore(weekEnd) ? endDate : weekEnd;

        return results.stream()
                .filter(r -> r.getResult() == MissionResult.SUCCESS)
                .filter(r -> !r.getMissionDate().isAfter(effectiveEnd))
                .count();
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

        return Arrays.stream(MissionType.values())
                .map(type -> TypeSuccessCount.builder()
                        .type(type)
                        .typeName(type.getDisplayName())
                        .successCount(successByType.getOrDefault(type, 0L).intValue())
                        .build())
                .toList();
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
        Optional<WeeklyAiAnalysis> analysisOpt = weeklyAiAnalysisRepository
                .findByUserUserIdAndWeekStartDate(userId, weekStartDate);

        if (analysisOpt.isEmpty()) {
            return AiFeedback.builder()
                    .failureReasonRanking(List.of())
                    .weeklyFeedback(null)
                    .dayOfWeekFeedbackTitle(null)
                    .dayOfWeekFeedbackContent("아직 AI 분석 결과가 생성되지 않았습니다.")
                    .mainFailureReason(null)
                    .overallFeedback("아직 AI 분석 결과가 생성되지 않았습니다.")
                    .build();
        }

        WeeklyAiAnalysis analysis = analysisOpt.get();
        List<AiFailureReasonRank> failureRanking = parseFailureReasonRanking(
                analysis.getFailureReasonRankingJson());

        return AiFeedback.builder()
                .failureReasonRanking(failureRanking)
                .weeklyFeedback(analysis.getWeeklyFeedback())
                .dayOfWeekFeedbackTitle(analysis.getDayOfWeekFeedbackTitle())
                .dayOfWeekFeedbackContent(analysis.getDayOfWeekFeedbackContent())
                .mainFailureReason(analysis.getMainFailureReason())
                .overallFeedback(analysis.getOverallFeedback())
                .build();
    }

    private List<AiFailureReasonRank> parseFailureReasonRanking(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<Map<String, Object>> rawList = objectMapper.readValue(json,
                    new TypeReference<List<Map<String, Object>>>() {});
            return rawList.stream()
                    .map(map -> AiFailureReasonRank.builder()
                            .rank(((Number) map.get("rank")).intValue())
                            .category((String) map.get("category"))
                            .count(((Number) map.get("count")).intValue())
                            .build())
                    .toList();
        } catch (JsonProcessingException e) {
            log.warn("실패 원인 순위 JSON 파싱 실패: {}", json, e);
            return List.of();
        }
    }
}
