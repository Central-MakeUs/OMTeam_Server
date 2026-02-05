package com.omteam.omt.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.omteam.omt.common.exception.BusinessException;
import com.omteam.omt.common.exception.ErrorCode;
import com.omteam.omt.user.domain.User;
import com.omteam.omt.user.domain.UserCharacter;
import com.omteam.omt.user.domain.UserOnboarding;
import com.omteam.omt.user.repository.UserCharacterRepository;
import com.omteam.omt.user.repository.UserOnboardingRepository;
import com.omteam.omt.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserQueryServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    UserOnboardingRepository userOnboardingRepository;

    @Mock
    UserCharacterRepository userCharacterRepository;

    UserQueryService userQueryService;

    final Long userId = 1L;

    @BeforeEach
    void setUp() {
        userQueryService = new UserQueryService(
                userRepository,
                userOnboardingRepository,
                userCharacterRepository
        );
    }

    @Test
    @DisplayName("getUser - 사용자 조회 성공")
    void getUser_success() {
        // given
        User user = createUser();
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        User result = userQueryService.getUser(userId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("getUser - 사용자 없으면 USER_NOT_FOUND 예외")
    void getUser_notFound_throwsException() {
        // given
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userQueryService.getUser(userId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("getActiveUser - 활성 사용자 조회 성공")
    void getActiveUser_success() {
        // given
        User user = createUser();
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        User result = userQueryService.getActiveUser(userId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("getActiveUser - 탈퇴한 사용자 조회 시 USER_ALREADY_WITHDRAWN 예외")
    void getActiveUser_withdrawn_throwsException() {
        // given
        User withdrawnUser = createWithdrawnUser();
        given(userRepository.findById(userId)).willReturn(Optional.of(withdrawnUser));

        // when & then
        assertThatThrownBy(() -> userQueryService.getActiveUser(userId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_ALREADY_WITHDRAWN);
    }

    @Test
    @DisplayName("getUserOnboarding - 온보딩 정보 조회 성공")
    void getUserOnboarding_success() {
        // given
        UserOnboarding onboarding = createUserOnboarding();
        given(userOnboardingRepository.findByUserId(userId)).willReturn(Optional.of(onboarding));

        // when
        UserOnboarding result = userQueryService.getUserOnboarding(userId);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getUserOnboarding - 온보딩 없으면 ONBOARDING_NOT_FOUND 예외")
    void getUserOnboarding_notFound_throwsException() {
        // given
        given(userOnboardingRepository.findByUserId(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userQueryService.getUserOnboarding(userId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ONBOARDING_NOT_FOUND);
    }

    @Test
    @DisplayName("getUserCharacter - 캐릭터 조회 성공")
    void getUserCharacter_success() {
        // given
        UserCharacter character = createUserCharacter();
        given(userCharacterRepository.findById(userId)).willReturn(Optional.of(character));

        // when
        UserCharacter result = userQueryService.getUserCharacter(userId);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getUserCharacter - 캐릭터 없으면 USER_NOT_FOUND 예외")
    void getUserCharacter_notFound_throwsException() {
        // given
        given(userCharacterRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userQueryService.getUserCharacter(userId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    /* ======================== */
    /* ===== Helper Zone ====== */
    /* ======================== */

    private User createUser() {
        return User.builder()
                .userId(userId)
                .email("test@example.com")
                .build();
    }

    private User createWithdrawnUser() {
        return User.builder()
                .userId(userId)
                .email("test@example.com")
                .deletedAt(LocalDateTime.now())
                .build();
    }

    private UserOnboarding createUserOnboarding() {
        return UserOnboarding.builder()
                .user(createUser())
                .appGoalText("건강 관리")
                .build();
    }

    private UserCharacter createUserCharacter() {
        return UserCharacter.builder()
                .userId(userId)
                .level(1)
                .successCount(0)
                .build();
    }
}
