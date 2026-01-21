package com.omteam.omt.security.auth.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import com.omteam.omt.common.exception.BusinessException;
import com.omteam.omt.common.exception.ErrorCode;
import com.omteam.omt.security.auth.dto.LoginResponse;
import com.omteam.omt.security.auth.jwt.JwtTokenProvider;
import com.omteam.omt.security.auth.oauth.OAuthClient;
import com.omteam.omt.security.auth.oauth.OAuthUserInfo;
import com.omteam.omt.user.domain.SocialProvider;
import com.omteam.omt.user.domain.User;
import com.omteam.omt.user.repository.UserRepository;
import com.omteam.omt.user.service.UserProvisioningService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock
    OAuthClient oAuthClient;
    @Mock
    UserProvisioningService userProvisioningService;
    @Mock
    UserRepository userRepository;
    @Mock
    JwtTokenProvider jwtTokenProvider;
    @Mock
    RefreshTokenService refreshTokenService;

    AuthService authService;

    final SocialProvider provider = SocialProvider.GOOGLE;
    final String idToken = "id-token";
    final String providerUserId = "provider-id";
    final String email = "test@test.com";

    @BeforeEach
    void setUp() {
        given(oAuthClient.getProvider()).willReturn(provider);

        authService = new AuthService(
                List.of(oAuthClient),
                userProvisioningService,
                userRepository,
                jwtTokenProvider,
                refreshTokenService
        );
    }

    @Test
    @DisplayName("로그인 성공 - 기존 사용자")
    void login_success_existing_user() {
        // given
        User user = createUser(true);
        mockOAuthUserInfo();
        given(userProvisioningService.findOrCreateUser(provider, providerUserId, email))
                .willReturn(user);
        mockToken();

        // when
        LoginResponse response = authService.login(provider, idToken);

        // then
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getExpiresIn()).isEqualTo(300L);
        assertThat(response.isOnboardingCompleted()).isTrue();
    }

    @Test
    @DisplayName("로그인 성공 - 신규 사용자 (온보딩 미완료)")
    void login_success_new_user() {
        // given
        User user = createUser(false);
        mockOAuthUserInfo();
        given(userProvisioningService.findOrCreateUser(provider, providerUserId, email))
                .willReturn(user);
        mockToken();

        // when
        LoginResponse response = authService.login(provider, idToken);

        // then
        assertThat(response.isOnboardingCompleted()).isFalse();
        then(userProvisioningService).should().findOrCreateUser(provider, providerUserId, email);
    }

    @Test
    @DisplayName("로그인 실패 - 지원하지 않는 OAuth 프로바이더")
    void login_fail_unsupported_provider() {
        // given
        SocialProvider unsupportedProvider = SocialProvider.KAKAO;

        // when & then
        assertThatThrownBy(() -> authService.login(unsupportedProvider, idToken))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.OAUTH_PROVIDER_NOT_FOUND);
    }

    @Test
    @DisplayName("로그인 시 UserProvisioningService 호출 검증")
    void login_calls_userProvisioningService() {
        // given
        User user = createUser(false);
        mockOAuthUserInfo();
        given(userProvisioningService.findOrCreateUser(provider, providerUserId, email))
                .willReturn(user);
        mockToken();

        // when
        authService.login(provider, idToken);

        // then
        then(userProvisioningService).should().findOrCreateUser(provider, providerUserId, email);
        then(jwtTokenProvider).should().createAccessToken(user.getUserId());
        then(jwtTokenProvider).should().createRefreshToken(user.getUserId());
    }

    @Test
    @DisplayName("로그아웃 성공")
    void logout_success() {
        // given
        Long userId = 1L;

        // when
        authService.logout(userId);

        // then
        then(refreshTokenService).should().deleteRefreshToken(userId);
    }

    @Test
    @DisplayName("토큰 갱신 성공")
    void refreshToken_success() {
        // given
        String refreshToken = "valid-refresh-token";
        Long userId = 1L;
        User user = createUser(true);

        given(jwtTokenProvider.validateToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.getUserId(refreshToken)).willReturn(userId);
        given(refreshTokenService.validateRefreshToken(userId, refreshToken)).willReturn(true);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        mockToken();

        // when
        LoginResponse response = authService.refreshToken(refreshToken);

        // then
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
    }

    @Test
    @DisplayName("토큰 갱신 실패 - 유효하지 않은 토큰")
    void refreshToken_fail_invalid_token() {
        // given
        String invalidToken = "invalid-token";
        given(jwtTokenProvider.validateToken(invalidToken)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.refreshToken(invalidToken))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("토큰 갱신 실패 - Redis에 저장된 토큰과 불일치")
    void refreshToken_fail_token_mismatch() {
        // given
        String refreshToken = "mismatched-token";
        Long userId = 1L;

        given(jwtTokenProvider.validateToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.getUserId(refreshToken)).willReturn(userId);
        given(refreshTokenService.validateRefreshToken(userId, refreshToken)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.refreshToken(refreshToken))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
    }

    /* ======================== */
    /* ===== Helper Zone ====== */
    /* ======================== */

    private void mockOAuthUserInfo() {
        OAuthUserInfo info = mock(OAuthUserInfo.class);
        given(info.getProviderUserId()).willReturn(providerUserId);
        given(info.getEmail()).willReturn(email);
        given(oAuthClient.getUserInfo(idToken)).willReturn(info);
    }

    private void mockToken() {
        given(jwtTokenProvider.createAccessToken(anyLong())).willReturn("access-token");
        given(jwtTokenProvider.createRefreshToken(anyLong())).willReturn("refresh-token");
        given(jwtTokenProvider.getAccessTokenExpireSeconds()).willReturn(300L);
        given(jwtTokenProvider.getRefreshTokenExpireSeconds()).willReturn(1209600L);
    }

    private User createUser(boolean onboardingCompleted) {
        User user = User.builder().email(email).build();
        user.setUserId(1L);
        user.setOnboardingCompleted(onboardingCompleted);
        return user;
    }
}
