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
import com.omteam.omt.user.service.UserProvisioningService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    OAuthClient oAuthClient;
    @Mock
    UserProvisioningService userProvisioningService;
    @Mock
    JwtTokenProvider jwtTokenProvider;

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
                jwtTokenProvider
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
    }

    private User createUser(boolean onboardingCompleted) {
        User user = User.builder().email(email).build();
        user.setUserId(1L);
        user.setOnboardingCompleted(onboardingCompleted);
        return user;
    }
}
