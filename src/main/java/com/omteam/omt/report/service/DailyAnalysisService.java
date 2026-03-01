package com.omteam.omt.report.service;

import com.omteam.omt.common.ai.dto.UserContext;
import com.omteam.omt.common.ai.service.UserContextService;
import com.omteam.omt.common.exception.BusinessException;
import com.omteam.omt.common.exception.ErrorCode;
import com.omteam.omt.report.client.AiDailyAnalysisClient;
import com.omteam.omt.report.constant.DefaultReportMessages;
import com.omteam.omt.report.dto.ReportDataStatus;
import com.omteam.omt.report.client.dto.AiDailyAnalysisRequest;
import com.omteam.omt.report.client.dto.AiDailyAnalysisResponse;
import com.omteam.omt.report.client.dto.EncouragementCandidate;
import com.omteam.omt.report.domain.DailyAnalysis;
import com.omteam.omt.report.domain.EncouragementIntent;
import com.omteam.omt.report.domain.EncouragementMessage;
import com.omteam.omt.report.dto.BatchProcessResult;
import com.omteam.omt.report.dto.DailyFeedbackResponse;
import com.omteam.omt.report.repository.DailyAnalysisRepository;
import com.omteam.omt.mission.domain.DailyMissionResult;
import com.omteam.omt.mission.repository.DailyMissionResultRepository;
import com.omteam.omt.user.domain.User;
import com.omteam.omt.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.ArrayList;
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
    private final DailyAnalysisRepository dailyAnalysisRepository;
    private final DailyMissionResultRepository missionResultRepository;
    private final UserRepository userRepository;
    private final UserContextService userContextService;

    /**
     * 모든 활성 사용자에 대해 오늘의 격려 메시지를 생성한다.
     */
    @Transactional
    public BatchProcessResult generateDailyEncouragementForAllUsers(LocalDate targetDate) {
        List<User> activeUsers = userRepository.findAll().stream()
                .filter(User::isActive)
                .filter(User::isOnboardingCompleted)
                .toList();

        int successCount = 0;
        List<Long> failedUserIds = new ArrayList<>();

        for (User user : activeUsers) {
            try {
                generateDailyAnalysisForUser(user, targetDate);
                successCount++;
            } catch (BusinessException e) {
                log.error("격려 메시지 생성 실패: userId={}", user.getUserId(), e);
                if (isAiServerError(e.getErrorCode())) {
                    failedUserIds.add(user.getUserId());
                }
            } catch (Exception e) {
                log.error("격려 메시지 생성 실패: userId={}", user.getUserId(), e);
            }
        }
        return BatchProcessResult.of(activeUsers.size(), successCount, failedUserIds);
    }

    private boolean isAiServerError(ErrorCode code) {
        return code == ErrorCode.AI_SERVER_ERROR
                || code == ErrorCode.AI_SERVER_CONNECTION_ERROR
                || code == ErrorCode.AI_SERVER_CIRCUIT_OPEN;
    }

    /**
     * 특정 사용자에 대해 데일리 분석 결과를 생성한다.
     */
    @Transactional
    public void generateDailyAnalysisForUser(User user, LocalDate targetDate) {
        if (dailyAnalysisRepository.existsByUserUserIdAndTargetDate(user.getUserId(), targetDate)) {
            log.debug("이미 분석 결과가 존재함: userId={}, targetDate={}", user.getUserId(), targetDate);
            return;
        }

        DailyMissionResult targetDateResult = missionResultRepository
                .findByUserUserIdAndMissionDate(user.getUserId(), targetDate)
                .orElse(null);

        AiDailyAnalysisRequest request = buildRequest(user.getUserId(), targetDate, targetDateResult);
        AiDailyAnalysisResponse response = aiDailyAnalysisClient.requestDailyAnalysis(request);

        DailyAnalysis dailyAnalysis = buildDailyAnalysis(user, targetDate, response);
        dailyAnalysisRepository.save(dailyAnalysis);

        log.info("데일리 분석 결과 생성 완료: userId={}, targetDate={}", user.getUserId(), targetDate);
    }

    private AiDailyAnalysisRequest buildRequest(Long userId, LocalDate targetDate, DailyMissionResult targetDateResult) {
        UserContext userContext = userContextService.buildContext(userId);

        AiDailyAnalysisRequest.AiDailyAnalysisRequestBuilder builder = AiDailyAnalysisRequest.builder()
                .userId(userId)
                .targetDate(targetDate.toString())
                .userContext(userContext);

        if (targetDateResult != null) {
            builder.todayMission(AiDailyAnalysisRequest.TodayMission.builder()
                    .missionType(targetDateResult.getMission().getType())
                    .difficulty(targetDateResult.getMission().getDifficulty())
                    .status(targetDateResult.getResult().name())
                    .failureReason(targetDateResult.getFailureReason())
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

    /**
     * 특정 날짜의 데일리 피드백을 조회한다.
     * 날짜가 null인 경우 오늘 날짜로 조회한다.
     * 피드백이 없는 경우 기본 안내 메시지를 반환한다.
     */
    @Transactional(readOnly = true)
    public DailyFeedbackResponse getDailyFeedback(Long userId, LocalDate date) {
        LocalDate targetDate = (date == null) ? LocalDate.now() : date;

        return dailyAnalysisRepository
                .findByUserUserIdAndTargetDate(userId, targetDate)
                .map(DailyFeedbackResponse::from)
                .orElseGet(() -> DailyFeedbackResponse.builder()
                        .dataStatus(ReportDataStatus.NO_DATA)
                        .targetDate(targetDate)
                        .feedbackText(DefaultReportMessages.DAILY_NO_DATA)
                        .isDefault(true)
                        .build());
    }
}
