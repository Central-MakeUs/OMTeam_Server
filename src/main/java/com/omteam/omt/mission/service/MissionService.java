package com.omteam.omt.mission.service;

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

    private static final int RECENT_HISTORY_DAYS = 7;

    public DailyMissionRecommendResponse recommendDailyMissions(Long userId) {
        LocalDate today = LocalDate.now();
        User user = findUserById(userId);

        // 이미 진행 중인 미션이 있는지 확인
        List<DailyRecommendedMission> inProgressMissions = recommendedMissionRepository
                .findByUserUserIdAndMissionDateAndStatus(userId, today, RecommendedMissionStatus.IN_PROGRESS);

        if (!inProgressMissions.isEmpty()) {
            DailyRecommendedMission inProgress = inProgressMissions.get(0);
            return DailyMissionRecommendResponse.builder()
                    .missionDate(today)
                    .recommendations(List.of())
                    .hasInProgressMission(true)
                    .inProgressMission(RecommendedMissionResponse.from(inProgress))
                    .build();
        }

        // 오늘 결과가 이미 있는지 확인
        if (missionResultRepository.existsByUserUserIdAndMissionDate(userId, today)) {
            throw new BusinessException(ErrorCode.DAILY_MISSION_ALREADY_EXISTS);
        }

        // 기존 추천 미션 만료 처리
        expireExistingRecommendations(userId, today);

        // AI 서버에서 새 미션 추천 받기
        AiMissionRecommendRequest request = buildAiRequest(userId, user);
        AiMissionRecommendResponse aiResponse = aiMissionClient.recommendDailyMissions(request);

        // 추천 미션 저장
        List<DailyRecommendedMission> recommendations = saveRecommendedMissions(user, today, aiResponse);

        return DailyMissionRecommendResponse.builder()
                .missionDate(today)
                .recommendations(RecommendedMissionResponse.fromList(recommendations))
                .hasInProgressMission(false)
                .inProgressMission(null)
                .build();
    }

    public RecommendedMissionResponse selectMission(Long userId, Long recommendedMissionId) {
        LocalDate today = LocalDate.now();

        // 이미 진행 중인 미션 확인
        if (hasInProgressMission(userId, today)) {
            throw new BusinessException(ErrorCode.MISSION_ALREADY_IN_PROGRESS);
        }

        // 오늘 결과가 이미 있는지 확인
        if (missionResultRepository.existsByUserUserIdAndMissionDate(userId, today)) {
            throw new BusinessException(ErrorCode.DAILY_MISSION_ALREADY_EXISTS);
        }

        DailyRecommendedMission recommendation = recommendedMissionRepository
                .findByIdAndUserUserId(recommendedMissionId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MISSION_NOT_FOUND));

        if (!recommendation.isSelectable()) {
            throw new BusinessException(ErrorCode.INVALID_MISSION_STATUS);
        }

        // 다른 추천 미션들의 선택 상태 해제 (같은 날짜)
        clearOtherSelections(userId, today, recommendedMissionId);

        recommendation.select();
        return RecommendedMissionResponse.from(recommendation);
    }

    public RecommendedMissionResponse startMission(Long userId) {
        LocalDate today = LocalDate.now();

        // 이미 진행 중인 미션 확인
        if (hasInProgressMission(userId, today)) {
            throw new BusinessException(ErrorCode.MISSION_ALREADY_IN_PROGRESS);
        }

        // 선택된 미션 찾기
        DailyRecommendedMission selectedMission = recommendedMissionRepository
                .findByUserUserIdAndMissionDateAndStatus(userId, today, RecommendedMissionStatus.SELECTED)
                .stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.MISSION_NOT_SELECTED));

        selectedMission.start();
        return RecommendedMissionResponse.from(selectedMission);
    }

    public RecommendedMissionResponse reselectMission(Long userId) {
        LocalDate today = LocalDate.now();

        // 진행 중인 미션 확인 - 진행 중이면 재선택 불가
        List<DailyRecommendedMission> inProgressMissions = recommendedMissionRepository
                .findByUserUserIdAndMissionDateAndStatus(userId, today, RecommendedMissionStatus.IN_PROGRESS);

        if (!inProgressMissions.isEmpty()) {
            // 진행 중인 미션을 RECOMMENDED 상태로 되돌림
            DailyRecommendedMission inProgress = inProgressMissions.get(0);
            inProgress.expire();  // 기존 진행 중 미션 만료
        }

        // 선택된 미션이 있으면 해제
        List<DailyRecommendedMission> selectedMissions = recommendedMissionRepository
                .findByUserUserIdAndMissionDateAndStatus(userId, today, RecommendedMissionStatus.SELECTED);

        for (DailyRecommendedMission selected : selectedMissions) {
            selected.expire();
        }

        // 추천 상태의 미션들 반환
        List<DailyRecommendedMission> recommendations = recommendedMissionRepository
                .findByUserUserIdAndMissionDateAndStatus(userId, today, RecommendedMissionStatus.RECOMMENDED);

        if (recommendations.isEmpty()) {
            throw new BusinessException(ErrorCode.NO_RECOMMENDED_MISSIONS);
        }

        return RecommendedMissionResponse.from(recommendations.get(0));
    }

    public MissionResultResponse completeMission(Long userId, MissionResultRequest request) {
        LocalDate today = LocalDate.now();

        // 오늘 결과가 이미 있는지 확인
        if (missionResultRepository.existsByUserUserIdAndMissionDate(userId, today)) {
            throw new BusinessException(ErrorCode.MISSION_RESULT_ALREADY_EXISTS);
        }

        // 진행 중인 미션 찾기
        DailyRecommendedMission inProgressMission = recommendedMissionRepository
                .findByUserUserIdAndMissionDateAndStatus(userId, today, RecommendedMissionStatus.IN_PROGRESS)
                .stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.MISSION_NOT_IN_PROGRESS));

        User user = findUserById(userId);

        // 결과 저장
        DailyMissionResult missionResult = DailyMissionResult.builder()
                .missionDate(today)
                .result(request.getResult())
                .failureReason(request.getResult() == MissionResult.FAILURE ? request.getFailureReason() : null)
                .mission(inProgressMission.getMission())
                .user(user)
                .build();

        missionResultRepository.save(missionResult);

        // 추천 미션 상태 완료로 변경
        inProgressMission.complete();

        return MissionResultResponse.from(missionResult);
    }

    @Transactional(readOnly = true)
    public TodayMissionStatusResponse getTodayMissionStatus(Long userId) {
        LocalDate today = LocalDate.now();

        // 오늘의 결과 확인
        var resultOpt = missionResultRepository.findByUserUserIdAndMissionDate(userId, today);
        if (resultOpt.isPresent()) {
            return TodayMissionStatusResponse.builder()
                    .date(today)
                    .hasRecommendations(false)
                    .hasSelectedMission(false)
                    .hasInProgressMission(false)
                    .hasCompletedMission(true)
                    .currentMission(null)
                    .missionResult(MissionResultResponse.from(resultOpt.get()))
                    .build();
        }

        // 진행 중인 미션 확인
        List<DailyRecommendedMission> inProgressMissions = recommendedMissionRepository
                .findByUserUserIdAndMissionDateAndStatus(userId, today, RecommendedMissionStatus.IN_PROGRESS);

        if (!inProgressMissions.isEmpty()) {
            return TodayMissionStatusResponse.builder()
                    .date(today)
                    .hasRecommendations(true)
                    .hasSelectedMission(false)
                    .hasInProgressMission(true)
                    .hasCompletedMission(false)
                    .currentMission(RecommendedMissionResponse.from(inProgressMissions.get(0)))
                    .missionResult(null)
                    .build();
        }

        // 선택된 미션 확인
        List<DailyRecommendedMission> selectedMissions = recommendedMissionRepository
                .findByUserUserIdAndMissionDateAndStatus(userId, today, RecommendedMissionStatus.SELECTED);

        if (!selectedMissions.isEmpty()) {
            return TodayMissionStatusResponse.builder()
                    .date(today)
                    .hasRecommendations(true)
                    .hasSelectedMission(true)
                    .hasInProgressMission(false)
                    .hasCompletedMission(false)
                    .currentMission(RecommendedMissionResponse.from(selectedMissions.get(0)))
                    .missionResult(null)
                    .build();
        }

        // 추천된 미션 확인
        List<DailyRecommendedMission> recommendations = recommendedMissionRepository
                .findByUserUserIdAndMissionDateAndStatus(userId, today, RecommendedMissionStatus.RECOMMENDED);

        return TodayMissionStatusResponse.builder()
                .date(today)
                .hasRecommendations(!recommendations.isEmpty())
                .hasSelectedMission(false)
                .hasInProgressMission(false)
                .hasCompletedMission(false)
                .currentMission(null)
                .missionResult(null)
                .build();
    }

    @Transactional(readOnly = true)
    public List<RecommendedMissionResponse> getTodayRecommendations(Long userId) {
        LocalDate today = LocalDate.now();

        List<DailyRecommendedMission> recommendations = recommendedMissionRepository
                .findActiveRecommendations(userId, today,
                        List.of(RecommendedMissionStatus.RECOMMENDED,
                                RecommendedMissionStatus.SELECTED,
                                RecommendedMissionStatus.IN_PROGRESS));

        return RecommendedMissionResponse.fromList(recommendations);
    }

    public int expireUncompletedMissions(LocalDate targetDate) {
        List<DailyRecommendedMission> uncompletedMissions = recommendedMissionRepository
                .findByMissionDateAndStatus(targetDate, RecommendedMissionStatus.IN_PROGRESS);

        int expiredCount = 0;
        for (DailyRecommendedMission mission : uncompletedMissions) {
            // 미션 상태를 만료로 변경
            mission.expire();

            // 실패 결과 저장
            DailyMissionResult failureResult = DailyMissionResult.builder()
                    .missionDate(targetDate)
                    .result(MissionResult.FAILURE)
                    .failureReason("시간 초과")
                    .mission(mission.getMission())
                    .user(mission.getUser())
                    .build();

            missionResultRepository.save(failureResult);
            expiredCount++;

            log.info("미션 만료 처리: userId={}, missionId={}, date={}",
                    mission.getUser().getUserId(), mission.getMission().getId(), targetDate);
        }

        return expiredCount;
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private void expireExistingRecommendations(Long userId, LocalDate date) {
        List<DailyRecommendedMission> existing = recommendedMissionRepository
                .findByUserUserIdAndMissionDateAndStatusNot(userId, date, RecommendedMissionStatus.EXPIRED);

        for (DailyRecommendedMission rec : existing) {
            if (rec.getStatus() != RecommendedMissionStatus.COMPLETED) {
                rec.expire();
            }
        }
    }

    private AiMissionRecommendRequest buildAiRequest(Long userId, User user) {
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
                .weeklyFailureReasons(weeklyFailureReasons != null ? weeklyFailureReasons : new ArrayList<>())
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

    private void clearOtherSelections(Long userId, LocalDate date, Long excludeId) {
        List<DailyRecommendedMission> selectedMissions = recommendedMissionRepository
                .findByUserUserIdAndMissionDateAndStatus(userId, date, RecommendedMissionStatus.SELECTED);

        for (DailyRecommendedMission selected : selectedMissions) {
            if (!selected.getId().equals(excludeId)) {
                // SELECTED 상태를 다시 RECOMMENDED로 변경
                selected.expire();
            }
        }
    }
}
