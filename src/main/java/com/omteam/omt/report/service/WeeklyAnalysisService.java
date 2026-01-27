package com.omteam.omt.report.service;

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
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeeklyAnalysisService {

    private final UserRepository userRepository;
    private final DailyMissionResultRepository missionResultRepository;
    private final WeeklyAiAnalysisRepository weeklyAiAnalysisRepository;
    private final AiWeeklyAnalysisClient aiWeeklyAnalysisClient;

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

        // 해당 주 실패 사유 수집
        List<String> failureReasons = collectFailureReasons(user.getUserId(), weekStartDate, weekEndDate);

        // AI 서버 호출
        AiWeeklyAnalysisRequest request = AiWeeklyAnalysisRequest.of(
                user.getUserId(),
                weekStartDate,
                weekEndDate,
                failureReasons
        );

        AiWeeklyAnalysisResponse response = aiWeeklyAnalysisClient.analyzeWeeklyMissions(request);

        // 결과 저장
        WeeklyAiAnalysis analysis = WeeklyAiAnalysis.builder()
                .user(user)
                .weekStartDate(weekStartDate)
                .mainFailureReason(response.getMainFailureReason())
                .overallFeedback(response.getOverallFeedback())
                .build();

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
}
