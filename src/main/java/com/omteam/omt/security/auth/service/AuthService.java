package com.omteam.omt.security.auth.service;

import com.omteam.omt.security.auth.dto.LoginResponse;
import com.omteam.omt.security.auth.jwt.JwtTokenProvider;
import com.omteam.omt.security.auth.oauth.OAuthClient;
import com.omteam.omt.security.auth.oauth.OAuthUserInfo;
import com.omteam.omt.user.domain.SocialProvider;
import com.omteam.omt.user.domain.User;
import com.omteam.omt.user.domain.UserCharacter;
import com.omteam.omt.user.domain.UserSocialAccount;
import com.omteam.omt.user.repository.UserCharacterRepository;
import com.omteam.omt.user.repository.UserRepository;
import com.omteam.omt.user.repository.UserSocialAccountRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final List<OAuthClient> oAuthClients;
    private final UserRepository userRepository;
    private final UserSocialAccountRepository socialAccountRepository;
    private final UserCharacterRepository characterRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public LoginResponse login(SocialProvider provider, String code) {
        OAuthClient client = oAuthClients.stream()
                .filter(c -> c.getProvider() == provider)
                .findFirst()
                .orElseThrow();

        OAuthUserInfo userInfo = client.getUserInfo(code);

        User user = findOrCreateUser(
                provider,
                userInfo.getProviderUserId(),
                userInfo.getEmail()
        );

        LoginResponse response = issueServerToken(user.getUserId());
        response.setOnboardingCompleted(user.isOnboardingCompleted());
        return response;
    }

    private User findOrCreateUser(
            SocialProvider provider,
            String providerUserId,
            String email
    ) {
        return socialAccountRepository
                .findByProviderAndProviderUserId(provider, providerUserId)
                .map(UserSocialAccount::getUser)
                .orElseGet(() -> createUser(provider, providerUserId, email));
    }

    private User createUser(
            SocialProvider provider,
            String providerUserId,
            String email
    ) {
        User user = userRepository.save(
                User.builder().email(email).build()
        );

        socialAccountRepository.save(
                UserSocialAccount.builder()
                        .provider(provider)
                        .providerUserId(providerUserId)
                        .user(user)
                        .build()
        );

        characterRepository.save(
                UserCharacter.builder()
                        .user(user)
                        .level(1)
                        .totalActiveDays(0)
                        .build()
        );

        return user;
    }

    private LoginResponse issueServerToken(Long userId) {
        String accessToken = jwtTokenProvider.createAccessToken(userId);
        String refreshToken = jwtTokenProvider.createRefreshToken(userId);

        return new LoginResponse(
                accessToken,
                refreshToken,
                3600
        );
    }
}
