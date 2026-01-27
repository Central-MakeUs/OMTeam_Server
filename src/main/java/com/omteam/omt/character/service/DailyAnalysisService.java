package com.omteam.omt.character.service;

import com.omteam.omt.character.client.AiDailyAnalysisClient;
import com.omteam.omt.character.client.dto.AiDailyAnalysisRequest;
import com.omteam.omt.character.client.dto.AiDailyAnalysisResponse;
import com.omteam.omt.character.client.dto.EncouragementCandidate;
import com.omteam.omt.character.domain.DailyAnalysis;
import com.omteam.omt.character.domain.EncouragementIntent;
import com.omteam.omt.character.domain.EncouragementMessage;
import com.omteam.omt.character.repository.DailyAnalysisRepository;
import com.omteam.omt.mission.domain.DailyMissionResult;
import com.omteam.omt.mission.repository.DailyMissionResultRepository;
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
public class DailyAnalysisService {

    private final AiDailyAnalysisClient aiDailyAnalysisClient;
    private final DailyAnalysisRepository DailyAnalysisRepository;
    private final DailyMissionResultRepository missionResultRepository;
    private final UserRepository userRepository;

    /**
     * 모든 활성 사용자에 대해 오늘의 격려 메시지를 생성한다.
     */
    @Transactional
    public int generateDailyEncouragementForAllUsers(LocalDate targetDate) {
        List<User> activeUsers = userRepository.findAll().stream()
                .filter(User::isActive)
                .filter(User::isOnboardingCompleted)
                .toList();

        int successCount = 0;
        for (User user : activeUsers) {
            try {
                generateDailyAnalysisForUser(user, targetDate);
                successCount++;
            } catch (Exception e) {
                log.error("격려 메시지 생성 실패: userId={}", user.getUserId(), e);
            }
        }
        return successCount;
    }

    /**
     * 특정 사용자에 대해 데일리 분석 결과를 생성한다.
     */
    @Transactional
    public void generateDailyAnalysisForUser(User user, LocalDate targetDate) {
        if (DailyAnalysisRepository.existsByUserUserIdAndTargetDate(user.getUserId(), targetDate)) {
            log.debug("이미 분석 결과가 존재함: userId={}, targetDate={}", user.getUserId(), targetDate);
            return;
        }

        LocalDate yesterday = targetDate.minusDays(1);
        DailyMissionResult yesterdayResult = missionResultRepository
                .findByUserUserIdAndMissionDate(user.getUserId(), yesterday)
                .orElse(null);

        AiDailyAnalysisRequest request = buildRequest(user.getUserId(), targetDate, yesterdayResult);
        AiDailyAnalysisResponse response = aiDailyAnalysisClient.requestDailyAnalysis(request);

        DailyAnalysis dailyAnalysis = buildDailyAnalysis(user, targetDate, response);
        DailyAnalysisRepository.save(dailyAnalysis);

        log.info("데일리 분석 결과 생성 완료: userId={}, targetDate={}", user.getUserId(), targetDate);
    }

    private AiDailyAnalysisRequest buildRequest(Long userId, LocalDate targetDate, DailyMissionResult yesterdayResult) {
        AiDailyAnalysisRequest.AiDailyAnalysisRequestBuilder builder = AiDailyAnalysisRequest.builder()
                .userId(userId)
                .targetDate(targetDate.toString());

        if (yesterdayResult != null) {
            builder.todayMission(AiDailyAnalysisRequest.TodayMission.builder()
                    .missionType(yesterdayResult.getMission().getType())
                    .difficulty(yesterdayResult.getMission().getDifficulty())
                    .result(yesterdayResult.getResult())
                    .failureReason(yesterdayResult.getFailureReason())
                    .build());
        }

        return builder.build();
    }

    private DailyAnalysis buildDailyAnalysis(
            User user,
            LocalDate targetDate,
            AiDailyAnalysisResponse response
    ) {
        return DailyAnalysis.builder()
                .user(user)
                .feedbackText(response.getFeedbackText())
                .targetDate(targetDate)
                .praise(toEncouragementMessage(
                        response.getEncouragementOf(EncouragementIntent.PRAISE)))
                .retry(toEncouragementMessage(
                        response.getEncouragementOf(EncouragementIntent.RETRY)))
                .normal(toEncouragementMessage(
                        response.getEncouragementOf(EncouragementIntent.NORMAL)))
                .push(toEncouragementMessage(
                        response.getEncouragementOf(EncouragementIntent.PUSH)))
                .build();
    }

    private EncouragementMessage toEncouragementMessage(EncouragementCandidate candidate) {
        if (candidate == null) {
            return null;
        }
        return EncouragementMessage.builder()
                .title(candidate.getTitle())
                .message(candidate.getMessage())
                .build();
    }
}
