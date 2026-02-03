package com.omteam.omt.security.auth.service;

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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final List<OAuthClient> oAuthClients;
    private final UserProvisioningService userProvisioningService;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

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

    public void logout(Long userId) {
        refreshTokenService.deleteRefreshToken(userId);
    }

    @Transactional(readOnly = true)
    public LoginResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        Long userId = jwtTokenProvider.getUserId(refreshToken);

        if (!refreshTokenService.validateRefreshToken(userId, refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.USER_ALREADY_WITHDRAWN);
        }

        return createLoginResponse(user);
    }

    @Transactional
    public void withdraw(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.USER_ALREADY_WITHDRAWN);
        }

        user.withdraw();
        refreshTokenService.deleteRefreshToken(userId);
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

        refreshTokenService.saveRefreshToken(
                user.getUserId(),
                refreshToken,
                jwtTokenProvider.getRefreshTokenExpireSeconds()
        );

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtTokenProvider.getAccessTokenExpireSeconds())
                .onboardingCompleted(user.isOnboardingCompleted())
                .build();
    }
}
