package com.omteam.omt.security.auth.service;

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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 서비스 - OAuth 검증 및 토큰 발급 담당
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final List<OAuthClient> oAuthClients;
    private final UserProvisioningService userProvisioningService;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public LoginResponse login(SocialProvider provider, String idToken) {
        OAuthClient client = findOAuthClient(provider);
        OAuthUserInfo userInfo = client.getUserInfo(idToken);

        User user = userProvisioningService.findOrCreateUser(
                provider,
                userInfo.getProviderUserId(),
                userInfo.getEmail()
        );

        return createLoginResponse(user);
    }

    private OAuthClient findOAuthClient(SocialProvider provider) {
        return oAuthClients.stream()
                .filter(c -> c.getProvider() == provider)
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.OAUTH_PROVIDER_NOT_FOUND));
    }

    private LoginResponse createLoginResponse(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getUserId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());

        LoginResponse response = new LoginResponse(
                accessToken,
                refreshToken,
                jwtTokenProvider.getAccessTokenExpireSeconds()
        );
        response.setOnboardingCompleted(user.isOnboardingCompleted());

        return response;
    }
}
