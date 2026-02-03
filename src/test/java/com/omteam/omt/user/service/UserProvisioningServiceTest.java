package com.omteam.omt.user.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import com.omteam.omt.user.domain.SocialProvider;
import com.omteam.omt.user.domain.User;
import com.omteam.omt.user.domain.UserCharacter;
import com.omteam.omt.user.domain.UserSocialAccount;
import com.omteam.omt.user.repository.UserCharacterRepository;
import com.omteam.omt.user.repository.UserRepository;
import com.omteam.omt.user.repository.UserSocialAccountRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserProvisioningServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    UserSocialAccountRepository socialAccountRepository;
    @Mock
    UserCharacterRepository characterRepository;

    UserProvisioningService userProvisioningService;

    final SocialProvider provider = SocialProvider.GOOGLE;
    final String providerUserId = "google-user-id";
    final String email = "test@example.com";

    @BeforeEach
    void setUp() {
        userProvisioningService = new UserProvisioningService(
                userRepository,
                socialAccountRepository,
                characterRepository
        );
    }

    @Test
    @DisplayName("기존 사용자 조회 - 소셜 계정이 존재하는 경우")
    void findOrCreateUser_existing_user() {
        // given
        User existingUser = createUser();
        UserSocialAccount socialAccount = UserSocialAccount.builder()
                .user(existingUser)
                .provider(provider)
                .providerUserId(providerUserId)
                .build();

        given(socialAccountRepository.findByProviderAndProviderUserId(provider, providerUserId))
                .willReturn(Optional.of(socialAccount));

        // when
        User result = userProvisioningService.findOrCreateUser(provider, providerUserId, email);

        // then
        assertThat(result).isEqualTo(existingUser);
        then(userRepository).shouldHaveNoInteractions();
        then(characterRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("신규 사용자 생성 - 소셜 계정이 없는 경우")
    void findOrCreateUser_new_user() {
        // given
        User savedUser = createUser();
        given(socialAccountRepository.findByProviderAndProviderUserId(provider, providerUserId))
                .willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        // when
        User result = userProvisioningService.findOrCreateUser(provider, providerUserId, email);

        // then
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo(email);
    }

    @Test
    @DisplayName("신규 사용자 생성 시 소셜 계정 저장")
    void findOrCreateUser_creates_social_account() {
        // given
        User savedUser = createUser();
        given(socialAccountRepository.findByProviderAndProviderUserId(provider, providerUserId))
                .willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        // when
        userProvisioningService.findOrCreateUser(provider, providerUserId, email);

        // then
        ArgumentCaptor<UserSocialAccount> captor = ArgumentCaptor.forClass(UserSocialAccount.class);
        then(socialAccountRepository).should().save(captor.capture());

        UserSocialAccount savedAccount = captor.getValue();
        assertThat(savedAccount.getProvider()).isEqualTo(provider);
        assertThat(savedAccount.getProviderUserId()).isEqualTo(providerUserId);
    }

    @Test
    @DisplayName("신규 사용자 생성 시 캐릭터 초기화 (레벨 1, 성공횟수 0)")
    void findOrCreateUser_creates_initial_character() {
        // given
        User savedUser = createUser();
        given(socialAccountRepository.findByProviderAndProviderUserId(provider, providerUserId))
                .willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        // when
        userProvisioningService.findOrCreateUser(provider, providerUserId, email);

        // then
        ArgumentCaptor<UserCharacter> captor = ArgumentCaptor.forClass(UserCharacter.class);
        then(characterRepository).should().save(captor.capture());

        UserCharacter savedCharacter = captor.getValue();
        assertThat(savedCharacter.getLevel()).isEqualTo(1);
        assertThat(savedCharacter.getSuccessCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("재가입 - 탈퇴한 사용자의 소셜 계정으로 로그인 시 새 User 생성 후 기존 SocialAccount 연결")
    void findOrCreateUser_creates_new_user_when_existing_is_withdrawn() {
        // given
        User withdrawnUser = spy(createUser());
        given(withdrawnUser.isActive()).willReturn(false);
        UserSocialAccount socialAccount = spy(UserSocialAccount.builder()
                .user(withdrawnUser)
                .provider(provider)
                .providerUserId(providerUserId)
                .build());

        User newUser = User.builder()
                .userId(2L)
                .email(email)
                .build();
        given(socialAccountRepository.findByProviderAndProviderUserId(provider, providerUserId))
                .willReturn(Optional.of(socialAccount));
        given(userRepository.save(any(User.class))).willReturn(newUser);

        // when
        User result = userProvisioningService.findOrCreateUser(provider, providerUserId, email);

        // then
        assertThat(result).isEqualTo(newUser);
        then(userRepository).should().save(any(User.class));
        then(socialAccount).should().updateUser(newUser);
        then(socialAccountRepository).should(never()).save(any(UserSocialAccount.class));
        then(characterRepository).should().save(any(UserCharacter.class));
    }

    private User createUser() {
        return User.builder()
                .userId(1L)
                .email(email)
                .build();
    }
}
