package com.omteam.omt.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class FcmConfig {

    @Value("${fcm.service-account-path:}")
    private String serviceAccountPath;

    @PostConstruct
    public void initFirebase() {
        if (serviceAccountPath.isBlank()) {
            log.warn("FCM_SERVICE_ACCOUNT_PATH is not configured. FCM push notifications will be disabled.");
            return;
        }

        if (!FirebaseApp.getApps().isEmpty()) {
            log.info("Firebase Admin SDK is already initialized.");
            return;
        }

        try (InputStream is = new FileInputStream(serviceAccountPath)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(is))
                    .build();
            FirebaseApp.initializeApp(options);
            log.info("Firebase Admin SDK initialized successfully.");
        } catch (IOException e) {
            log.error("Failed to initialize Firebase Admin SDK. FCM push notifications will be disabled: {}", e.getMessage());
        }
    }
}
