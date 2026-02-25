package com.omteam.omt.notification.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.omteam.omt.common.exception.BusinessException;
import com.omteam.omt.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FcmService {

    public void sendNotification(String token, String title, String body) {
        if (FirebaseApp.getApps().isEmpty()) {
            log.debug("Firebase not initialized. Skipping FCM notification.");
            return;
        }

        if (token == null || token.isBlank()) {
            log.warn("FCM token is null or blank. Skipping notification.");
            return;
        }

        Message message = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .build();

        try {
            String messageId = FirebaseMessaging.getInstance().send(message);
            log.info("FCM notification sent successfully. messageId={}", messageId);
        } catch (FirebaseMessagingException e) {
            log.warn("FCM send failed [token={}]: errorCode={}, msg={}",
                    token, e.getMessagingErrorCode(), e.getMessage());
            throw new BusinessException(ErrorCode.FCM_SEND_FAILED);
        }
    }
}
