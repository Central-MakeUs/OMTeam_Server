package com.omteam.omt.report.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omteam.omt.common.util.DateRangeUtils;
import com.omteam.omt.report.client.dto.AiWeeklyAnalysisResponse;
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
        LocalDate weekEnd = DateRangeUtils.getWeekEndDate(effectiveStart);
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

        // 4. AI 피드백 (DB에서 조회)
        AiFeedback feedback = getAiFeedback(userId, effectiveStart);

        // 5. 실패 원인 순위 (AI 분석 결과에서 추출)
        List<FailureReasonRank> failureRanks = feedback.failureReasonRanking().stream()
                .map(aiRank -> FailureReasonRank.builder()
                        .rank(aiRank.rank())
                        .reason(aiRank.category())
                        .count(aiRank.count())
                        .build())
                .toList();

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
            return DateRangeUtils.getWeekStartDate(LocalDate.now());
        }
        return DateRangeUtils.getWeekStartOfMonth(year, month, weekOfMonth);
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

    private AiFeedback getAiFeedback(Long userId, LocalDate weekStartDate) {
        Optional<WeeklyAiAnalysis> analysisOpt = weeklyAiAnalysisRepository
                .findByUserUserIdAndWeekStartDate(userId, weekStartDate);

        if (analysisOpt.isEmpty()) {
            return AiFeedback.builder()
                    .failureReasonRanking(List.of())
                    .weeklyFeedback(null)
                    .build();
        }

        WeeklyAiAnalysis analysis = analysisOpt.get();
        List<AiFailureReasonRank> failureRanking = parseFailureReasonRanking(
                analysis.getFailureReasonRankingJson());

        return AiFeedback.builder()
                .failureReasonRanking(failureRanking)
                .weeklyFeedback(analysis.getWeeklyFeedback())
                .build();
    }

    private List<AiFailureReasonRank> parseFailureReasonRanking(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<AiWeeklyAnalysisResponse.FailureReasonRank> rawList = objectMapper.readValue(json,
                    new TypeReference<List<AiWeeklyAnalysisResponse.FailureReasonRank>>() {});
            return rawList.stream()
                    .map(rankData -> AiFailureReasonRank.builder()
                            .rank(rankData.getRank())
                            .category(rankData.getCategory())
                            .count(rankData.getCount())
                            .build())
                    .toList();
        } catch (JsonProcessingException e) {
            log.warn("실패 원인 순위 JSON 파싱 실패: {}", json, e);
            return List.of();
        }
    }
}
