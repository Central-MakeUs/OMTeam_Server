package com.omteam.omt.common.ai.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import com.omteam.omt.common.ai.dto.UserContext;
import com.omteam.omt.common.exception.BusinessException;
import com.omteam.omt.common.exception.ErrorCode;
import com.omteam.omt.mission.domain.DailyMissionResult;
import com.omteam.omt.mission.domain.MissionResult;
import com.omteam.omt.mission.repository.DailyMissionResultRepository;
import com.omteam.omt.user.domain.LifestyleType;
import com.omteam.omt.user.domain.User;
import com.omteam.omt.user.domain.UserCharacter;
import com.omteam.omt.user.domain.UserOnboarding;
import com.omteam.omt.user.repository.UserCharacterRepository;
import com.omteam.omt.user.repository.UserOnboardingRepository;
import com.omteam.omt.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserContextServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    UserOnboardingRepository onboardingRepository;
    @Mock
    UserCharacterRepository characterRepository;
    @Mock
    DailyMissionResultRepository missionResultRepository;

    UserContextService userContextService;

    final Long userId = 1L;

    @BeforeEach
    void setUp() {
        userContextService = new UserContextService(
                userRepository,
                onboardingRepository,
                characterRepository,
                missionResultRepository
        );
    }

    @Test
    @DisplayName("사용자가 모든 데이터를 가진 경우 buildContext 성공")
    void buildContext_success_with_all_data() {
        // given
        User user = createUser("테스트유저");
        UserOnboarding onboarding = createOnboarding(user, "건강 관리", "요가, 스트레칭", LifestyleType.REGULAR_DAYTIME);
        UserCharacter character = createCharacter(2, 35);
        List<DailyMissionResult> results = createMissionResults(5, 2); // 5 성공, 2 실패

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(onboardingRepository.findByUserId(userId)).willReturn(Optional.of(onboarding));
        given(characterRepository.findById(userId)).willReturn(Optional.of(character));
        given(missionResultRepository.findByUserUserIdAndMissionDateBetween(eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(results);

        // when
        UserContext context = userContextService.buildContext(userId);

        // then
        assertThat(context.getNickname()).isEqualTo("테스트유저");
        assertThat(context.getAppGoal()).isEqualTo("건강 관리");
        assertThat(context.getPreferredExercise()).isEqualTo("요가, 스트레칭");
        assertThat(context.getLifestyleType()).isEqualTo("REGULAR_DAYTIME");
        assertThat(context.getCurrentLevel()).isEqualTo(2);
        assertThat(context.getSuccessCount()).isEqualTo(35);
        assertThat(context.getRecentMissionSuccessRate()).isEqualTo(5.0 / 7.0);
    }

    @Test
    @DisplayName("사용자가 온보딩 데이터가 없는 경우 관련 필드 null 처리")
    void buildContext_without_onboarding() {
        // given
        User user = createUser("테스트유저");
        UserCharacter character = createCharacter(1, 10);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(onboardingRepository.findByUserId(userId)).willReturn(Optional.empty());
        given(characterRepository.findById(userId)).willReturn(Optional.of(character));
        given(missionResultRepository.findByUserUserIdAndMissionDateBetween(eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(Collections.emptyList());

        // when
        UserContext context = userContextService.buildContext(userId);

        // then
        assertThat(context.getNickname()).isEqualTo("테스트유저");
        assertThat(context.getAppGoal()).isNull();
        assertThat(context.getPreferredExercise()).isNull();
        assertThat(context.getLifestyleType()).isNull();
        assertThat(context.getCurrentLevel()).isEqualTo(1);
        assertThat(context.getSuccessCount()).isEqualTo(10);
    }

    @Test
    @DisplayName("사용자가 캐릭터 데이터가 없는 경우 레벨 1, 성공 횟수 0 반환")
    void buildContext_without_character() {
        // given
        User user = createUser("테스트유저");
        UserOnboarding onboarding = createOnboarding(user, "체중 감량", "걷기", LifestyleType.IRREGULAR_OVERTIME);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(onboardingRepository.findByUserId(userId)).willReturn(Optional.of(onboarding));
        given(characterRepository.findById(userId)).willReturn(Optional.empty());
        given(missionResultRepository.findByUserUserIdAndMissionDateBetween(eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(Collections.emptyList());

        // when
        UserContext context = userContextService.buildContext(userId);

        // then
        assertThat(context.getNickname()).isEqualTo("테스트유저");
        assertThat(context.getAppGoal()).isEqualTo("체중 감량");
        assertThat(context.getCurrentLevel()).isEqualTo(1);
        assertThat(context.getSuccessCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("사용자가 존재하지 않는 경우 BusinessException(USER_NOT_FOUND) 발생")
    void buildContext_fail_user_not_found() {
        // given
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userContextService.buildContext(userId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("미션 결과가 있는 경우 성공률 계산 (성공/실패 혼합)")
    void calculateSuccessRate_with_mixed_results() {
        // given
        User user = createUser("테스트유저");
        List<DailyMissionResult> results = createMissionResults(4, 3); // 4 성공, 3 실패

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(onboardingRepository.findByUserId(userId)).willReturn(Optional.empty());
        given(characterRepository.findById(userId)).willReturn(Optional.empty());
        given(missionResultRepository.findByUserUserIdAndMissionDateBetween(eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(results);

        // when
        UserContext context = userContextService.buildContext(userId);

        // then
        assertThat(context.getRecentMissionSuccessRate()).isEqualTo(4.0 / 7.0);
    }

    @Test
    @DisplayName("미션 결과가 없는 경우 성공률 null 반환")
    void calculateSuccessRate_no_results() {
        // given
        User user = createUser("테스트유저");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(onboardingRepository.findByUserId(userId)).willReturn(Optional.empty());
        given(characterRepository.findById(userId)).willReturn(Optional.empty());
        given(missionResultRepository.findByUserUserIdAndMissionDateBetween(eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(Collections.emptyList());

        // when
        UserContext context = userContextService.buildContext(userId);

        // then
        assertThat(context.getRecentMissionSuccessRate()).isNull();
    }

    @Test
    @DisplayName("모든 미션 성공한 경우 성공률 1.0")
    void calculateSuccessRate_all_success() {
        // given
        User user = createUser("테스트유저");
        List<DailyMissionResult> results = createMissionResults(7, 0); // 7 성공, 0 실패

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(onboardingRepository.findByUserId(userId)).willReturn(Optional.empty());
        given(characterRepository.findById(userId)).willReturn(Optional.empty());
        given(missionResultRepository.findByUserUserIdAndMissionDateBetween(eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(results);

        // when
        UserContext context = userContextService.buildContext(userId);

        // then
        assertThat(context.getRecentMissionSuccessRate()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("모든 미션 실패한 경우 성공률 0.0")
    void calculateSuccessRate_all_failure() {
        // given
        User user = createUser("테스트유저");
        List<DailyMissionResult> results = createMissionResults(0, 7); // 0 성공, 7 실패

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(onboardingRepository.findByUserId(userId)).willReturn(Optional.empty());
        given(characterRepository.findById(userId)).willReturn(Optional.empty());
        given(missionResultRepository.findByUserUserIdAndMissionDateBetween(eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(results);

        // when
        UserContext context = userContextService.buildContext(userId);

        // then
        assertThat(context.getRecentMissionSuccessRate()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("온보딩 데이터의 lifestyleType이 null인 경우 lifestyleType null 반환")
    void buildContext_with_null_lifestyleType() {
        // given
        User user = createUser("테스트유저");
        UserOnboarding onboarding = createOnboarding(user, "근력 증가", "웨이트", null);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(onboardingRepository.findByUserId(userId)).willReturn(Optional.of(onboarding));
        given(characterRepository.findById(userId)).willReturn(Optional.empty());
        given(missionResultRepository.findByUserUserIdAndMissionDateBetween(eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(Collections.emptyList());

        // when
        UserContext context = userContextService.buildContext(userId);

        // then
        assertThat(context.getAppGoal()).isEqualTo("근력 증가");
        assertThat(context.getPreferredExercise()).isEqualTo("웨이트");
        assertThat(context.getLifestyleType()).isNull();
    }

    /* ======================== */
    /* ===== Helper Zone ====== */
    /* ======================== */

    private User createUser(String nickname) {
        return User.builder()
                .userId(userId)
                .nickname(nickname)
                .email("test@example.com")
                .onboardingCompleted(true)
                .build();
    }

    private UserOnboarding createOnboarding(User user, String appGoalText, String preferredExerciseText, LifestyleType lifestyleType) {
        return UserOnboarding.builder()
                .userId(userId)
                .user(user)
                .appGoalText(appGoalText)
                .preferredExerciseText(preferredExerciseText)
                .lifestyleType(lifestyleType)
                .build();
    }

    private UserCharacter createCharacter(int level, int successCount) {
        return UserCharacter.builder()
                .userId(userId)
                .level(level)
                .successCount(successCount)
                .build();
    }

    private List<DailyMissionResult> createMissionResults(int successCount, int failureCount) {
        List<DailyMissionResult> results = new java.util.ArrayList<>();

        // 성공 결과 추가
        for (int i = 0; i < successCount; i++) {
            results.add(DailyMissionResult.builder()
                    .id((long) (i + 1))
                    .missionDate(LocalDate.now().minusDays(i + 1))
                    .result(MissionResult.SUCCESS)
                    .build());
        }

        // 실패 결과 추가
        for (int i = 0; i < failureCount; i++) {
            results.add(DailyMissionResult.builder()
                    .id((long) (successCount + i + 1))
                    .missionDate(LocalDate.now().minusDays(successCount + i + 1))
                    .result(MissionResult.FAILURE)
                    .failureReason("시간 부족")
                    .build());
        }

        return results;
    }
}
