package com.omteam.omt.report.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omteam.omt.common.ai.dto.UserContext;
import com.omteam.omt.common.ai.service.UserContextService;
import com.omteam.omt.mission.domain.DailyMissionResult;
import com.omteam.omt.mission.domain.MissionResult;
import com.omteam.omt.mission.repository.DailyMissionResultRepository;
import com.omteam.omt.report.client.AiWeeklyAnalysisClient;
import com.omteam.omt.report.client.dto.AiWeeklyAnalysisRequest;
import com.omteam.omt.report.client.dto.AiWeeklyAnalysisResponse;
import com.omteam.omt.report.domain.WeeklyAiAnalysis;
import com.omteam.omt.report.repository.WeeklyAiAnalysisRepository;
import com.omteam.omt.user.domain.User;
import com.omteam.omt.user.repository.UserRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeeklyAnalysisService {

    private static final int MONTHLY_PATTERN_DAYS = 30;

    private final UserRepository userRepository;
    private final DailyMissionResultRepository missionResultRepository;
    private final WeeklyAiAnalysisRepository weeklyAiAnalysisRepository;
    private final AiWeeklyAnalysisClient aiWeeklyAnalysisClient;
    private final UserContextService userContextService;
    private final ObjectMapper objectMapper;

    public void generateWeeklyAnalysisForAllUsers(LocalDate weekStartDate) {
        LocalDate weekEndDate = weekStartDate.plusDays(6);

        List<User> activeUsers = userRepository.findAllByDeletedAtIsNull();
        log.info("주간 분석 시작: {} ~ {}, 대상 사용자 수: {}",
                weekStartDate, weekEndDate, activeUsers.size());

        int successCount = 0;
        int failCount = 0;

        for (User user : activeUsers) {
            try {
                generateWeeklyAnalysisForUser(user, weekStartDate, weekEndDate);
                successCount++;
            } catch (Exception e) {
                log.error("사용자 {} 주간 분석 실패: {}", user.getUserId(), e.getMessage());
                failCount++;
            }
        }

        log.info("주간 분석 완료: 성공={}, 실패={}", successCount, failCount);
    }

    @Transactional
    public void generateWeeklyAnalysisForUser(User user, LocalDate weekStartDate, LocalDate weekEndDate) {
        // 이미 분석 결과가 있으면 스킵
        if (weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(user.getUserId(), weekStartDate).isPresent()) {
            log.debug("사용자 {} 의 {} 주차 분석 결과가 이미 존재합니다.", user.getUserId(), weekStartDate);
            return;
        }

        // UserContext 생성
        UserContext userContext = userContextService.buildContext(user.getUserId());

        // 해당 주 실패 사유 수집
        List<String> failureReasons = collectFailureReasons(user.getUserId(), weekStartDate, weekEndDate);

        // 주간 일별 결과 수집
        List<AiWeeklyAnalysisRequest.DailyResultSummary> weeklyResults =
                collectWeeklyResults(user.getUserId(), weekStartDate, weekEndDate);

        // 월간 요일별 통계 수집
        List<AiWeeklyAnalysisRequest.DayOfWeekStats> monthlyStats =
                collectMonthlyDayOfWeekStats(user.getUserId());

        // AI 서버 호출
        AiWeeklyAnalysisRequest request = AiWeeklyAnalysisRequest.of(
                user.getUserId(),
                userContext,
                weekStartDate,
                weekEndDate,
                failureReasons,
                weeklyResults,
                monthlyStats
        );

        AiWeeklyAnalysisResponse response = aiWeeklyAnalysisClient.analyzeWeeklyMissions(request);

        // 결과 저장
        WeeklyAiAnalysis analysis = buildWeeklyAiAnalysis(user, weekStartDate, response);

        weeklyAiAnalysisRepository.save(analysis);
        log.debug("사용자 {} 주간 분석 저장 완료", user.getUserId());
    }

    private List<String> collectFailureReasons(Long userId, LocalDate startDate, LocalDate endDate) {
        return missionResultRepository.findByUserUserIdAndMissionDateBetween(userId, startDate, endDate)
                .stream()
                .filter(result -> result.getResult() == MissionResult.FAILURE)
                .map(DailyMissionResult::getFailureReason)
                .filter(reason -> reason != null && !reason.isBlank())
                .toList();
    }

    private List<AiWeeklyAnalysisRequest.DailyResultSummary> collectWeeklyResults(
            Long userId, LocalDate startDate, LocalDate endDate) {
        List<DailyMissionResult> results = missionResultRepository
                .findByUserUserIdAndMissionDateBetween(userId, startDate, endDate);

        Map<LocalDate, DailyMissionResult> resultMap = results.stream()
                .collect(Collectors.toMap(DailyMissionResult::getMissionDate, r -> r, (a, b) -> a));

        List<AiWeeklyAnalysisRequest.DailyResultSummary> summaries = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            DailyMissionResult result = resultMap.get(date);
            summaries.add(AiWeeklyAnalysisRequest.DailyResultSummary.builder()
                    .date(date)
                    .dayOfWeek(date.getDayOfWeek().name())
                    .status(result != null ? result.getResult().name() : "NOT_PERFORMED")
                    .missionType(result != null && result.getMission() != null
                            ? result.getMission().getType().name() : null)
                    .failureReason(result != null ? result.getFailureReason() : null)
                    .build());
        }
        return summaries;
    }

    private List<AiWeeklyAnalysisRequest.DayOfWeekStats> collectMonthlyDayOfWeekStats(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate monthAgo = today.minusDays(MONTHLY_PATTERN_DAYS);

        List<DailyMissionResult> results = missionResultRepository
                .findByUserUserIdAndMissionDateBetween(userId, monthAgo, today);

        Map<DayOfWeek, List<DailyMissionResult>> resultsByDayOfWeek = results.stream()
                .collect(Collectors.groupingBy(r -> r.getMissionDate().getDayOfWeek()));

        return Arrays.stream(DayOfWeek.values())
                .map(dow -> {
                    List<DailyMissionResult> dowResults = resultsByDayOfWeek.getOrDefault(dow, List.of());
                    long successCount = dowResults.stream()
                            .filter(r -> r.getResult() == MissionResult.SUCCESS)
                            .count();
                    double successRate = dowResults.isEmpty() ? 0.0
                            : (double) successCount / dowResults.size() * 100;

                    return AiWeeklyAnalysisRequest.DayOfWeekStats.builder()
                            .dayOfWeek(dow.name())
                            .totalCount(dowResults.size())
                            .successCount((int) successCount)
                            .successRate(successRate)
                            .build();
                })
                .toList();
    }

    private WeeklyAiAnalysis buildWeeklyAiAnalysis(User user, LocalDate weekStartDate,
            AiWeeklyAnalysisResponse response) {
        String failureRankingJson = null;
        if (response.getFailureReasonRanking() != null) {
            try {
                failureRankingJson = objectMapper.writeValueAsString(response.getFailureReasonRanking());
            } catch (JsonProcessingException e) {
                log.warn("실패 원인 순위 JSON 변환 실패: {}", e.getMessage());
            }
        }

        return WeeklyAiAnalysis.builder()
                .user(user)
                .weekStartDate(weekStartDate)
                // 새로운 필드
                .failureReasonRankingJson(failureRankingJson)
                .weeklyFeedback(response.getWeeklyFeedback())
                .dayOfWeekFeedbackTitle(response.getDayOfWeekFeedback() != null
                        ? response.getDayOfWeekFeedback().getTitle() : null)
                .dayOfWeekFeedbackContent(response.getDayOfWeekFeedback() != null
                        ? response.getDayOfWeekFeedback().getContent() : null)
                // 하위 호환성
                .mainFailureReason(response.getMainFailureReason())
                .overallFeedback(response.getOverallFeedback())
                .build();
    }
}
