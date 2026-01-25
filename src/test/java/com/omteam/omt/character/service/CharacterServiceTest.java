package com.omteam.omt.character.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import com.omteam.omt.character.domain.DailyEncouragementSet;
import com.omteam.omt.character.domain.EncouragementMessage;
import com.omteam.omt.character.dto.CharacterResponse;
import com.omteam.omt.character.repository.DailyEncouragementSetRepository;
import com.omteam.omt.common.exception.BusinessException;
import com.omteam.omt.common.exception.ErrorCode;
import com.omteam.omt.mission.domain.DailyMissionResult;
import com.omteam.omt.mission.domain.MissionResult;
import com.omteam.omt.mission.repository.DailyMissionResultRepository;
import com.omteam.omt.mission.repository.DailyRecommendedMissionRepository;
import com.omteam.omt.user.domain.User;
import com.omteam.omt.user.domain.UserCharacter;
import com.omteam.omt.user.repository.UserCharacterRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CharacterServiceTest {

    @Mock
    UserCharacterRepository characterRepository;
    @Mock
    DailyEncouragementSetRepository encouragementSetRepository;
    @Mock
    DailyMissionResultRepository missionResultRepository;
    @Mock
    DailyRecommendedMissionRepository recommendedMissionRepository;

    CharacterService characterService;

    final Long userId = 1L;

    @BeforeEach
    void setUp() {
        characterService = new CharacterService(
                characterRepository,
                encouragementSetRepository,
                missionResultRepository,
                recommendedMissionRepository
        );
    }

    @Test
    @DisplayName("미션 성공 기록 시 캐릭터의 successCount가 증가한다")
    void recordMissionSuccess_increases_count() {
        // given
        UserCharacter character = createCharacter(1, 5);
        given(characterRepository.findById(userId)).willReturn(Optional.of(character));

        // when
        characterService.recordMissionSuccess(userId);

        // then
        assertThat(character.getSuccessCount()).isEqualTo(6);
    }

    @Test
    @DisplayName("미션 성공 기록 시 30회 도달하면 레벨업")
    void recordMissionSuccess_level_up_at_30() {
        // given
        UserCharacter character = createCharacter(1, 29);
        given(characterRepository.findById(userId)).willReturn(Optional.of(character));

        // when
        characterService.recordMissionSuccess(userId);

        // then
        assertThat(character.getLevel()).isEqualTo(2);
        assertThat(character.getSuccessCount()).isEqualTo(30);
    }

    @Test
    @DisplayName("미션 성공 기록 실패 - 사용자 없음")
    void recordMissionSuccess_fail_user_not_found() {
        // given
        given(characterRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> characterService.recordMissionSuccess(userId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("캐릭터 정보 조회 - 격려 메시지 포함")
    void getCharacterInfo_with_encouragement() {
        // given
        UserCharacter character = createCharacter(2, 45);
        DailyEncouragementSet encouragementSet = createEncouragementSet();
        DailyMissionResult yesterdayResult = createMissionResult(MissionResult.SUCCESS);

        given(characterRepository.findById(userId)).willReturn(Optional.of(character));
        given(encouragementSetRepository.findByUserUserIdAndTargetDate(eq(userId), any(LocalDate.class)))
                .willReturn(Optional.of(encouragementSet));
        given(missionResultRepository.findByUserUserIdAndMissionDate(eq(userId), any(LocalDate.class)))
                .willReturn(Optional.of(yesterdayResult));

        // when
        CharacterResponse response = characterService.getCharacterInfo(userId);

        // then
        assertThat(response.level()).isEqualTo(2);
        assertThat(response.successCount()).isEqualTo(45);
        assertThat(response.experiencePercent()).isEqualTo(50); // 45 % 30 = 15, 15/30 * 100 = 50
        assertThat(response.encouragementTitle()).isEqualTo("잘하고 있어요");
        assertThat(response.encouragementMessage()).isEqualTo("이대로만 하면 목표에 도달할 수 있어요.");
    }

    @Test
    @DisplayName("캐릭터 정보 조회 - 격려 메시지 없으면 기본 메시지")
    void getCharacterInfo_default_encouragement() {
        // given
        UserCharacter character = createCharacter(1, 10);

        given(characterRepository.findById(userId)).willReturn(Optional.of(character));
        given(encouragementSetRepository.findByUserUserIdAndTargetDate(eq(userId), any(LocalDate.class)))
                .willReturn(Optional.empty());

        // when
        CharacterResponse response = characterService.getCharacterInfo(userId);

        // then
        assertThat(response.encouragementTitle()).isEqualTo("오늘도 힘내세요!");
        assertThat(response.encouragementMessage()).isEqualTo("작은 시작이 큰 변화를 만들어요.");
    }

    @Test
    @DisplayName("캐릭터 정보 조회 실패 - 사용자 없음")
    void getCharacterInfo_fail_user_not_found() {
        // given
        given(characterRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> characterService.getCharacterInfo(userId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    /* ======================== */
    /* ===== Helper Zone ====== */
    /* ======================== */

    private UserCharacter createCharacter(int level, int successCount) {
        return UserCharacter.builder()
                .userId(userId)
                .level(level)
                .successCount(successCount)
                .build();
    }

    private DailyEncouragementSet createEncouragementSet() {
        return DailyEncouragementSet.builder()
                .targetDate(LocalDate.now())
                .praise(EncouragementMessage.builder()
                        .title("잘하고 있어요")
                        .message("이대로만 하면 목표에 도달할 수 있어요.")
                        .build())
                .retry(EncouragementMessage.builder()
                        .title("다시 시작해봐요")
                        .message("내일은 더 잘할 수 있어요.")
                        .build())
                .normal(EncouragementMessage.builder()
                        .title("조금씩 성장하고 있어요")
                        .message("꾸준함이 가장 중요해요.")
                        .build())
                .push(EncouragementMessage.builder()
                        .title("오늘 시작해봐요")
                        .message("작은 미션부터 시작해보세요.")
                        .build())
                .build();
    }

    private DailyMissionResult createMissionResult(MissionResult result) {
        return DailyMissionResult.builder()
                .missionDate(LocalDate.now().minusDays(1))
                .result(result)
                .build();
    }
}
