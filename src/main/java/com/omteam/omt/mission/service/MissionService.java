package com.omteam.omt.mission.service;

import com.omteam.omt.character.service.CharacterService;
import com.omteam.omt.common.exception.BusinessException;
import com.omteam.omt.common.exception.ErrorCode;
import com.omteam.omt.mission.client.AiMissionClient;
import com.omteam.omt.mission.client.dto.AiMissionRecommendRequest;
import com.omteam.omt.mission.client.dto.AiMissionRecommendRequest.MissionHistory;
import com.omteam.omt.mission.client.dto.AiMissionRecommendRequest.OnboardingData;
import com.omteam.omt.mission.client.dto.AiMissionRecommendResponse;
import com.omteam.omt.mission.domain.DailyMissionResult;
import com.omteam.omt.mission.domain.DailyRecommendedMission;
import com.omteam.omt.mission.domain.Mission;
import com.omteam.omt.mission.domain.MissionResult;
import com.omteam.omt.mission.domain.RecommendedMissionStatus;
import com.omteam.omt.mission.dto.DailyMissionRecommendResponse;
import com.omteam.omt.mission.dto.MissionResultRequest;
import com.omteam.omt.mission.dto.MissionResultResponse;
import com.omteam.omt.mission.dto.RecommendedMissionResponse;
import com.omteam.omt.mission.dto.TodayMissionStatusResponse;
import com.omteam.omt.mission.repository.DailyMissionResultRepository;
import com.omteam.omt.mission.repository.DailyRecommendedMissionRepository;
import com.omteam.omt.mission.repository.MissionRepository;
import com.omteam.omt.user.domain.User;
import com.omteam.omt.user.domain.UserOnboarding;
import com.omteam.omt.user.repository.UserOnboardingRepository;
import com.omteam.omt.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MissionService {

    private final MissionRepository missionRepository;
    private final DailyRecommendedMissionRepository recommendedMissionRepository;
    private final DailyMissionResultRepository missionResultRepository;
    private final UserRepository userRepository;
    private final UserOnboardingRepository userOnboardingRepository;
    private final AiMissionClient aiMissionClient;
    private final CharacterService characterService;

    private static final int RECENT_HISTORY_DAYS = 7;

    public DailyMissionRecommendResponse recommendDailyMissions(Long userId) {
        LocalDate today = LocalDate.now();
        User user = findUserById(userId);

        // 오늘 이미 완료된 미션이 있으면 추천 불가
        validateNoMissionResultToday(userId, today);

        // 기존 미션(IN_PROGRESS, RECOMMENDED)이 있으면 모두 만료 처리 후 새로 추천
        expireExistingRecommendations(userId, today);

        // AI 서버에서 새 미션 추천 받기
        AiMissionRecommendRequest request = buildAiRequest(userId);
        AiMissionRecommendResponse aiResponse = aiMissionClient.recommendDailyMissions(request);
        List<DailyRecommendedMission> recommendations = saveRecommendedMissions(user, today, aiResponse);

        return DailyMissionRecommendResponse.builder()
                .missionDate(today)
                .recommendations(RecommendedMissionResponse.fromList(recommendations))
                .hasInProgressMission(false)
                .inProgressMission(null)
                .build();
    }

    public RecommendedMissionResponse startMission(Long userId, Long missionId) {
        LocalDate today = LocalDate.now();

        validateNoMissionResultToday(userId, today);

        DailyRecommendedMission newMission = recommendedMissionRepository
                .findByIdAndUserUserId(missionId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MISSION_NOT_FOUND));

        if (!newMission.isStartable()) {
            throw new BusinessException(ErrorCode.INVALID_MISSION_STATUS);
        }

        // 진행 중인 미션이 있으면 자동 만료 처리
        findFirstMissionByStatus(userId, today, RecommendedMissionStatus.IN_PROGRESS)
                .ifPresent(DailyRecommendedMission::expire);

        newMission.start();
        return RecommendedMissionResponse.from(newMission);
    }

    public MissionResultResponse completeMission(Long userId, MissionResultRequest request) {
        LocalDate today = LocalDate.now();

        validateNoMissionResultTodayForComplete(userId, today);

        DailyRecommendedMission inProgressMission = getInProgressMissionOrThrow(userId, today);
        User user = findUserById(userId);

        DailyMissionResult missionResult = DailyMissionResult.builder()
                .missionDate(today)
                .result(request.getResult())
                .failureReason(request.getResult() == MissionResult.FAILURE ? request.getFailureReason() : null)
                .mission(inProgressMission.getMission())
                .user(user)
                .build();

        missionResultRepository.save(missionResult);
        inProgressMission.complete();

        // 미션 성공 시 캐릭터 경험치 증가
        if (request.getResult() == MissionResult.SUCCESS) {
            characterService.recordMissionSuccess(userId);
        }

        return MissionResultResponse.from(missionResult);
    }

    @Transactional(readOnly = true)
    public TodayMissionStatusResponse getTodayMissionStatus(Long userId) {
        LocalDate today = LocalDate.now();

        // 오늘의 결과 확인 - 완료된 경우
        var resultOpt = missionResultRepository.findByUserUserIdAndMissionDate(userId, today);
        if (resultOpt.isPresent()) {
            return buildStatusResponse(today, false, false, true, null, MissionResultResponse.from(resultOpt.get()));
        }

        // 진행 중인 미션 확인
        Optional<DailyRecommendedMission> inProgress = findFirstMissionByStatus(userId, today, RecommendedMissionStatus.IN_PROGRESS);
        if (inProgress.isPresent()) {
            return buildStatusResponse(today, true, true, false, RecommendedMissionResponse.from(inProgress.get()), null);
        }

        // 추천된 미션만 있거나 없는 경우
        boolean hasRecommendations = !findMissionsByStatus(userId, today, RecommendedMissionStatus.RECOMMENDED).isEmpty();
        return buildStatusResponse(today, hasRecommendations, false, false, null, null);
    }

    private TodayMissionStatusResponse buildStatusResponse(
            LocalDate date,
            boolean hasRecommendations,
            boolean hasInProgressMission,
            boolean hasCompletedMission,
            RecommendedMissionResponse currentMission,
            MissionResultResponse missionResult) {
        return TodayMissionStatusResponse.builder()
                .date(date)
                .hasRecommendations(hasRecommendations)
                .hasInProgressMission(hasInProgressMission)
                .hasCompletedMission(hasCompletedMission)
                .currentMission(currentMission)
                .missionResult(missionResult)
                .build();
    }

    @Transactional(readOnly = true)
    public List<RecommendedMissionResponse> getTodayRecommendations(Long userId) {
        LocalDate today = LocalDate.now();

        List<DailyRecommendedMission> recommendations = recommendedMissionRepository
                .findActiveRecommendations(userId, today,
                        List.of(RecommendedMissionStatus.RECOMMENDED,
                                RecommendedMissionStatus.IN_PROGRESS,
                                RecommendedMissionStatus.COMPLETED));

        return RecommendedMissionResponse.fromList(recommendations);
    }

    public int expireUncompletedMissions(LocalDate targetDate) {
        List<DailyRecommendedMission> uncompletedMissions = recommendedMissionRepository
                .findByMissionDateAndStatus(targetDate, RecommendedMissionStatus.IN_PROGRESS);

        uncompletedMissions.forEach(mission -> expireMissionWithFailureResult(mission, targetDate));

        return uncompletedMissions.size();
    }

    private void expireMissionWithFailureResult(DailyRecommendedMission mission, LocalDate targetDate) {
        mission.expire();

        DailyMissionResult failureResult = DailyMissionResult.builder()
                .missionDate(targetDate)
                .result(MissionResult.FAILURE)
                .failureReason("시간 초과")
                .mission(mission.getMission())
                .user(mission.getUser())
                .build();

        missionResultRepository.save(failureResult);

        log.info("미션 만료 처리: userId={}, missionId={}, date={}",
                mission.getUser().getUserId(), mission.getMission().getId(), targetDate);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private void expireExistingRecommendations(Long userId, LocalDate date) {
        recommendedMissionRepository
                .findByUserUserIdAndMissionDateAndStatusNot(userId, date, RecommendedMissionStatus.EXPIRED)
                .stream()
                .filter(rec -> rec.getStatus() != RecommendedMissionStatus.COMPLETED)
                .forEach(DailyRecommendedMission::expire);
    }

    private AiMissionRecommendRequest buildAiRequest(Long userId) {
        UserOnboarding onboarding = userOnboardingRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ONBOARDING_NOT_FOUND));

        LocalDate weekAgo = LocalDate.now().minusDays(RECENT_HISTORY_DAYS);

        // 최근 미션 히스토리 조회
        List<DailyMissionResult> recentResults = missionResultRepository
                .findRecentResults(userId, weekAgo);

        List<MissionHistory> missionHistories = recentResults.stream()
                .map(result -> MissionHistory.of(
                        result.getMissionDate(),
                        result.getMission().getType(),
                        result.getMission().getDifficulty(),
                        result.getResult(),
                        result.getFailureReason()
                ))
                .toList();

        // 주간 실패 사유 조회
        LocalDate today = LocalDate.now();
        List<String> weeklyFailureReasons = missionResultRepository
                .findFailureReasonsByUserIdAndDateRange(userId, MissionResult.FAILURE, weekAgo);

        OnboardingData onboardingData = OnboardingData.from(
                onboarding.getAppGoalText(),
                onboarding.getWorkTimeType(),
                onboarding.getAvailableStartTime(),
                onboarding.getAvailableEndTime(),
                onboarding.getMinExerciseMinutes(),
                onboarding.getPreferredExerciseText(),
                onboarding.getLifestyleType()
        );

        return AiMissionRecommendRequest.builder()
                .userId(userId)
                .onboarding(onboardingData)
                .recentMissionHistory(missionHistories)
                .weeklyFailureReasons(weeklyFailureReasons)
                .build();
    }

    private List<DailyRecommendedMission> saveRecommendedMissions(
            User user, LocalDate date, AiMissionRecommendResponse aiResponse) {

        List<DailyRecommendedMission> recommendations = new ArrayList<>();

        for (var recommendedMission : aiResponse.getMissions()) {
            Mission mission = missionRepository.save(recommendedMission.toEntity());

            DailyRecommendedMission drm = DailyRecommendedMission.builder()
                    .missionDate(date)
                    .status(RecommendedMissionStatus.RECOMMENDED)
                    .mission(mission)
                    .user(user)
                    .build();

            recommendations.add(recommendedMissionRepository.save(drm));
        }

        return recommendations;
    }

    private boolean hasInProgressMission(Long userId, LocalDate date) {
        return recommendedMissionRepository.existsByUserUserIdAndMissionDateAndStatusIn(
                userId, date, List.of(RecommendedMissionStatus.IN_PROGRESS));
    }

    // ==================== 상태별 미션 조회 Helper ====================

    private List<DailyRecommendedMission> findMissionsByStatus(Long userId, LocalDate date, RecommendedMissionStatus status) {
        return recommendedMissionRepository.findByUserUserIdAndMissionDateAndStatus(userId, date, status);
    }

    private Optional<DailyRecommendedMission> findFirstMissionByStatus(Long userId, LocalDate date, RecommendedMissionStatus status) {
        return findMissionsByStatus(userId, date, status).stream().findFirst();
    }

    private void validateNoMissionResultToday(Long userId, LocalDate date) {
        if (missionResultRepository.existsByUserUserIdAndMissionDate(userId, date)) {
            throw new BusinessException(ErrorCode.DAILY_MISSION_ALREADY_EXISTS);
        }
    }

    private void validateNoMissionResultTodayForComplete(Long userId, LocalDate date) {
        if (missionResultRepository.existsByUserUserIdAndMissionDate(userId, date)) {
            throw new BusinessException(ErrorCode.MISSION_RESULT_ALREADY_EXISTS);
        }
    }

    private DailyRecommendedMission getInProgressMissionOrThrow(Long userId, LocalDate date) {
        return findFirstMissionByStatus(userId, date, RecommendedMissionStatus.IN_PROGRESS)
                .orElseThrow(() -> new BusinessException(ErrorCode.MISSION_NOT_IN_PROGRESS));
    }
}
