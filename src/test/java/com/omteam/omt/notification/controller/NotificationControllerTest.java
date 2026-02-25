package com.omteam.omt.notification.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;

import com.omteam.omt.common.response.ApiResponse;
import com.omteam.omt.notification.dto.FcmTokenRequest;
import com.omteam.omt.notification.service.NotificationService;
import com.omteam.omt.security.principal.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("[단위] NotificationController")
class NotificationControllerTest {

    @Mock
    NotificationService notificationService;

    @InjectMocks
    NotificationController notificationController;

    UserPrincipal principal;

    @BeforeEach
    void setUp() {
        principal = new UserPrincipal(1L);
    }

    @Nested
    @DisplayName("PUT /api/notification/fcm-token - FCM 토큰 등록/갱신")
    class RegisterFcmToken {

        @Test
        @DisplayName("성공 - FCM 토큰이 등록된다")
        void success() {
            // given
            FcmTokenRequest request = new FcmTokenRequest();
            request.setFcmToken("valid-fcm-token");

            willDoNothing().given(notificationService).registerFcmToken(1L, "valid-fcm-token");

            // when
            ApiResponse<Void> result = notificationController.registerFcmToken(principal, request);

            // then
            assertThat(result.success()).isTrue();
            assertThat(result.data()).isNull();
            then(notificationService).should().registerFcmToken(1L, "valid-fcm-token");
        }

        @Test
        @DisplayName("성공 - 긴 FCM 토큰도 등록된다")
        void success_longToken() {
            // given
            String longToken = "a".repeat(512);
            FcmTokenRequest request = new FcmTokenRequest();
            request.setFcmToken(longToken);

            willDoNothing().given(notificationService).registerFcmToken(1L, longToken);

            // when
            ApiResponse<Void> result = notificationController.registerFcmToken(principal, request);

            // then
            assertThat(result.success()).isTrue();
            then(notificationService).should().registerFcmToken(1L, longToken);
        }
    }

    @Nested
    @DisplayName("DELETE /api/notification/fcm-token - FCM 토큰 삭제")
    class DeleteFcmToken {

        @Test
        @DisplayName("성공 - FCM 토큰이 삭제된다")
        void success() {
            // given
            willDoNothing().given(notificationService).deleteFcmToken(1L);

            // when
            ApiResponse<Void> result = notificationController.deleteFcmToken(principal);

            // then
            assertThat(result.success()).isTrue();
            assertThat(result.data()).isNull();
            then(notificationService).should().deleteFcmToken(1L);
        }

        @Test
        @DisplayName("성공 - 다른 userId로 호출 시 해당 사용자 토큰이 삭제된다")
        void success_differentUser() {
            // given
            UserPrincipal otherPrincipal = new UserPrincipal(99L);
            willDoNothing().given(notificationService).deleteFcmToken(99L);

            // when
            ApiResponse<Void> result = notificationController.deleteFcmToken(otherPrincipal);

            // then
            assertThat(result.success()).isTrue();
            then(notificationService).should().deleteFcmToken(99L);
        }
    }
}
