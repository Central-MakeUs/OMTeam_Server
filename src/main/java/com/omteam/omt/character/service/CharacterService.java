package com.omteam.omt.character.service;

import com.omteam.omt.character.domain.DailyAnalysis;
import com.omteam.omt.character.domain.EncouragementIntent;
import com.omteam.omt.character.domain.EncouragementMessage;
import com.omteam.omt.character.dto.CharacterResponse;
import com.omteam.omt.character.repository.DailyAnalysisRepository;
import com.omteam.omt.common.exception.BusinessException;
import com.omteam.omt.common.exception.ErrorCode;
import com.omteam.omt.mission.domain.DailyMissionResult;
import com.omteam.omt.mission.domain.MissionResult;
import com.omteam.omt.mission.domain.RecommendedMissionStatus;
import com.omteam.omt.mission.repository.DailyMissionResultRepository;
import com.omteam.omt.mission.repository.DailyRecommendedMissionRepository;
import com.omteam.omt.user.domain.UserCharacter;
import com.omteam.omt.user.repository.UserCharacterRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CharacterService {

    private static final EncouragementMessage DEFAULT_ENCOURAGEMENT = EncouragementMessage.builder()
            .title("오늘도 힘내세요!")
            .message("작은 시작이 큰 변화를 만들어요.")
            .build();

    private final UserCharacterRepository characterRepository;
    private final DailyAnalysisRepository encouragementSetRepository;
    private final DailyMissionResultRepository missionResultRepository;
    private final DailyRecommendedMissionRepository recommendedMissionRepository;

    /**
     * 미션 성공 시 호출. 경험치 증가 및 레벨업 처리.
     */
    @Transactional
    public void recordMissionSuccess(Long userId) {
        UserCharacter character = characterRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        character.recordMissionSuccess();
        log.info("미션 성공 기록: userId={}, level={}, successCount={}",
                userId, character.getLevel(), character.getSuccessCount());
    }

    /**
     * 캐릭터 정보 조회 (레벨, 경험치, 격려 메시지 포함)
     */
    @Transactional(readOnly = true)
    public CharacterResponse getCharacterInfo(Long userId) {
        UserCharacter character = characterRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        LocalDate today = LocalDate.now();
        EncouragementMessage encouragement = getEncouragementMessage(userId, today);

        return CharacterResponse.builder()
                .level(character.getLevel())
                .experiencePercent(character.getExperiencePercent())
                .successCount(character.getSuccessCount())
                .successCountUntilNextLevel(character.getSuccessCountUntilNextLevel())
                .encouragementTitle(encouragement.getTitle())
                .encouragementMessage(encouragement.getMessage())
                .build();
    }

    private EncouragementMessage getEncouragementMessage(Long userId, LocalDate today) {
        Optional<DailyAnalysis> encouragementSetOpt =
                encouragementSetRepository.findByUserUserIdAndTargetDate(userId, today);

        if (encouragementSetOpt.isEmpty()) {
            return DEFAULT_ENCOURAGEMENT;
        }

        DailyAnalysis encouragementSet = encouragementSetOpt.get();
        EncouragementIntent intent = determineIntent(userId, today);
        EncouragementMessage message = encouragementSet.getMessageByIntent(intent);

        return message != null ? message : DEFAULT_ENCOURAGEMENT;
    }

    /**
     * 사용자 상태에 따라 적절한 격려 메시지 intent를 결정한다.
     */
    private EncouragementIntent determineIntent(Long userId, LocalDate today) {
        LocalDate yesterday = today.minusDays(1);

        // 어제 미션 결과 확인
        Optional<DailyMissionResult> yesterdayResultOpt =
                missionResultRepository.findByUserUserIdAndMissionDate(userId, yesterday);

        if (yesterdayResultOpt.isPresent()) {
            DailyMissionResult result = yesterdayResultOpt.get();
            if (result.getResult() == MissionResult.SUCCESS) {
                return EncouragementIntent.PRAISE;
            } else {
                return EncouragementIntent.RETRY;
            }
        }

        // 어제 미션을 시작했는지 확인 (결과가 없는 경우)
        List<RecommendedMissionStatus> activeStatuses = List.of(
                RecommendedMissionStatus.IN_PROGRESS,
                RecommendedMissionStatus.COMPLETED
        );
        boolean hadActiveMission = recommendedMissionRepository
                .existsByUserUserIdAndMissionDateAndStatusIn(userId, yesterday, activeStatuses);

        if (!hadActiveMission) {
            // 어제 미션을 아예 선택/시작하지 않음
            return EncouragementIntent.PUSH;
        }

        // 기본값
        return EncouragementIntent.NORMAL;
    }
}
