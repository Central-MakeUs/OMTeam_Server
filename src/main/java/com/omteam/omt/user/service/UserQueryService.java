package com.omteam.omt.user.service;

import com.omteam.omt.common.exception.BusinessException;
import com.omteam.omt.common.exception.ErrorCode;
import com.omteam.omt.user.domain.User;
import com.omteam.omt.user.domain.UserCharacter;
import com.omteam.omt.user.domain.UserOnboarding;
import com.omteam.omt.user.repository.UserCharacterRepository;
import com.omteam.omt.user.repository.UserOnboardingRepository;
import com.omteam.omt.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryService {

    private final UserRepository userRepository;
    private final UserOnboardingRepository userOnboardingRepository;
    private final UserCharacterRepository userCharacterRepository;

    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    public User getActiveUser(Long userId) {
        User user = getUser(userId);
        if (user.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.USER_ALREADY_WITHDRAWN);
        }
        return user;
    }

    public UserOnboarding getUserOnboarding(Long userId) {
        return userOnboardingRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ONBOARDING_NOT_FOUND));
    }

    public UserCharacter getUserCharacter(Long userId) {
        return userCharacterRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
