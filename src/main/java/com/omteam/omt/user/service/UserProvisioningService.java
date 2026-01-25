package com.omteam.omt.user.service;

import com.omteam.omt.user.domain.SocialProvider;
import com.omteam.omt.user.domain.User;
import com.omteam.omt.user.domain.UserCharacter;
import com.omteam.omt.user.domain.UserSocialAccount;
import com.omteam.omt.user.repository.UserCharacterRepository;
import com.omteam.omt.user.repository.UserRepository;
import com.omteam.omt.user.repository.UserSocialAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 계정 생성 및 초기화 담당 서비스
 */
@Service
@RequiredArgsConstructor
public class UserProvisioningService {

    private static final int INITIAL_CHARACTER_LEVEL = 1;
    private static final int INITIAL_SUCCESS_COUNT = 0;

    private final UserRepository userRepository;
    private final UserSocialAccountRepository socialAccountRepository;
    private final UserCharacterRepository characterRepository;

    /**
     * 소셜 계정으로 사용자를 조회하거나, 없으면 새로 생성한다.
     */
    @Transactional
    public User findOrCreateUser(SocialProvider provider, String providerUserId, String email) {
        return socialAccountRepository
                .findByProviderAndProviderUserId(provider, providerUserId)
                .map(UserSocialAccount::getUser)
                .orElseGet(() -> createNewUser(provider, providerUserId, email));
    }

    private User createNewUser(SocialProvider provider, String providerUserId, String email) {
        User user = userRepository.save(
                User.builder().email(email).build()
        );

        createSocialAccount(user, provider, providerUserId);
        createInitialCharacter(user);

        return user;
    }

    private void createSocialAccount(User user, SocialProvider provider, String providerUserId) {
        socialAccountRepository.save(
                UserSocialAccount.builder()
                        .provider(provider)
                        .providerUserId(providerUserId)
                        .user(user)
                        .build()
        );
    }

    private void createInitialCharacter(User user) {
        characterRepository.save(
                UserCharacter.builder()
                        .user(user)
                        .level(INITIAL_CHARACTER_LEVEL)
                        .successCount(INITIAL_SUCCESS_COUNT)
                        .build()
        );
    }
}
