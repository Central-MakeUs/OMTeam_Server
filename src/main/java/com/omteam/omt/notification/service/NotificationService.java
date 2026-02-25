package com.omteam.omt.notification.service;

import com.omteam.omt.user.domain.User;
import com.omteam.omt.user.service.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final UserQueryService userQueryService;

    @Transactional
    public void registerFcmToken(Long userId, String fcmToken) {
        User user = userQueryService.getUser(userId);
        user.updateFcmToken(fcmToken);
    }

    @Transactional
    public void deleteFcmToken(Long userId) {
        User user = userQueryService.getUser(userId);
        user.updateFcmToken(null);
    }
}
