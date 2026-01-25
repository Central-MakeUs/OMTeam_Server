package com.omteam.omt.user.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserCharacterTest {

    @Test
    @DisplayName("미션 성공 기록 시 successCount가 증가한다")
    void recordMissionSuccess_increases_successCount() {
        // given
        UserCharacter character = createCharacter(1, 0);

        // when
        character.recordMissionSuccess();

        // then
        assertThat(character.getSuccessCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("30회 미션 성공 시 레벨업이 발생한다")
    void recordMissionSuccess_level_up_at_30() {
        // given
        UserCharacter character = createCharacter(1, 29);

        // when
        character.recordMissionSuccess();

        // then
        assertThat(character.getLevel()).isEqualTo(2);
        assertThat(character.getSuccessCount()).isEqualTo(30);
    }

    @Test
    @DisplayName("60회 미션 성공 시 레벨 3이 된다")
    void recordMissionSuccess_level_up_at_60() {
        // given
        UserCharacter character = createCharacter(2, 59);

        // when
        character.recordMissionSuccess();

        // then
        assertThat(character.getLevel()).isEqualTo(3);
        assertThat(character.getSuccessCount()).isEqualTo(60);
    }

    @Test
    @DisplayName("경험치 퍼센트 계산 - 0회 성공 시 0%")
    void getExperiencePercent_zero() {
        // given
        UserCharacter character = createCharacter(1, 0);

        // when
        int percent = character.getExperiencePercent();

        // then
        assertThat(percent).isEqualTo(0);
    }

    @Test
    @DisplayName("경험치 퍼센트 계산 - 15회 성공 시 50%")
    void getExperiencePercent_fifty() {
        // given
        UserCharacter character = createCharacter(1, 15);

        // when
        int percent = character.getExperiencePercent();

        // then
        assertThat(percent).isEqualTo(50);
    }

    @Test
    @DisplayName("경험치 퍼센트 계산 - 29회 성공 시 96%")
    void getExperiencePercent_almost_full() {
        // given
        UserCharacter character = createCharacter(1, 29);

        // when
        int percent = character.getExperiencePercent();

        // then
        assertThat(percent).isEqualTo(96);
    }

    @Test
    @DisplayName("경험치 퍼센트 계산 - 레벨업 후 0%로 리셋")
    void getExperiencePercent_reset_after_level_up() {
        // given
        UserCharacter character = createCharacter(2, 30);

        // when
        int percent = character.getExperiencePercent();

        // then
        assertThat(percent).isEqualTo(0);
    }

    @Test
    @DisplayName("경험치 퍼센트 계산 - 레벨업 후 5회 추가 성공 시 16%")
    void getExperiencePercent_after_level_up_with_progress() {
        // given
        UserCharacter character = createCharacter(2, 35);

        // when
        int percent = character.getExperiencePercent();

        // then
        assertThat(percent).isEqualTo(16);
    }

    @Test
    @DisplayName("다음 레벨까지 남은 성공 횟수 계산 - 0회 시 30회 남음")
    void getSuccessCountUntilNextLevel_from_zero() {
        // given
        UserCharacter character = createCharacter(1, 0);

        // when
        int remaining = character.getSuccessCountUntilNextLevel();

        // then
        assertThat(remaining).isEqualTo(30);
    }

    @Test
    @DisplayName("다음 레벨까지 남은 성공 횟수 계산 - 25회 시 5회 남음")
    void getSuccessCountUntilNextLevel_almost_level_up() {
        // given
        UserCharacter character = createCharacter(1, 25);

        // when
        int remaining = character.getSuccessCountUntilNextLevel();

        // then
        assertThat(remaining).isEqualTo(5);
    }

    private UserCharacter createCharacter(int level, int successCount) {
        return UserCharacter.builder()
                .level(level)
                .successCount(successCount)
                .build();
    }
}
