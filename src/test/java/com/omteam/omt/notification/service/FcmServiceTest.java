package com.omteam.omt.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.omteam.omt.common.exception.BusinessException;
import com.omteam.omt.common.exception.ErrorCode;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("[단위] FcmService")
class FcmServiceTest {

    final FcmService fcmService = new FcmService();

    @Nested
    @DisplayName("sendNotification")
    class SendNotification {

        @Test
        @DisplayName("Firebase 미초기화 시 noop - 예외 없이 리턴")
        void noop_whenFirebaseNotInitialized() {
            try (MockedStatic<FirebaseApp> mockedFirebaseApp = mockStatic(FirebaseApp.class)) {
                mockedFirebaseApp.when(FirebaseApp::getApps).thenReturn(Collections.emptyList());

                // when & then - 예외 없이 정상 종료
                fcmService.sendNotification("some-token", "제목", "본문");
            }
        }

        @Test
        @DisplayName("null 토큰 시 noop - 예외 없이 리턴")
        void noop_whenTokenIsNull() throws FirebaseMessagingException {
            try (MockedStatic<FirebaseApp> mockedFirebaseApp = mockStatic(FirebaseApp.class);
                 MockedStatic<FirebaseMessaging> mockedMessaging = mockStatic(FirebaseMessaging.class)) {

                mockedFirebaseApp.when(FirebaseApp::getApps).thenReturn(List.of(org.mockito.Mockito.mock(FirebaseApp.class)));
                FirebaseMessaging mockMessaging = org.mockito.Mockito.mock(FirebaseMessaging.class);
                mockedMessaging.when(FirebaseMessaging::getInstance).thenReturn(mockMessaging);

                // when & then
                fcmService.sendNotification(null, "제목", "본문");

                verify(mockMessaging, never()).send(any(Message.class));
            }
        }

        @Test
        @DisplayName("빈 토큰 시 noop - 예외 없이 리턴")
        void noop_whenTokenIsBlank() throws FirebaseMessagingException {
            try (MockedStatic<FirebaseApp> mockedFirebaseApp = mockStatic(FirebaseApp.class);
                 MockedStatic<FirebaseMessaging> mockedMessaging = mockStatic(FirebaseMessaging.class)) {

                mockedFirebaseApp.when(FirebaseApp::getApps).thenReturn(List.of(org.mockito.Mockito.mock(FirebaseApp.class)));
                FirebaseMessaging mockMessaging = org.mockito.Mockito.mock(FirebaseMessaging.class);
                mockedMessaging.when(FirebaseMessaging::getInstance).thenReturn(mockMessaging);

                // when & then
                fcmService.sendNotification("   ", "제목", "본문");

                verify(mockMessaging, never()).send(any(Message.class));
            }
        }

        @Test
        @DisplayName("성공 - messageId 반환")
        void success_sendMessage() throws FirebaseMessagingException {
            try (MockedStatic<FirebaseApp> mockedFirebaseApp = mockStatic(FirebaseApp.class);
                 MockedStatic<FirebaseMessaging> mockedMessaging = mockStatic(FirebaseMessaging.class)) {

                mockedFirebaseApp.when(FirebaseApp::getApps).thenReturn(List.of(org.mockito.Mockito.mock(FirebaseApp.class)));
                FirebaseMessaging mockMessaging = org.mockito.Mockito.mock(FirebaseMessaging.class);
                mockedMessaging.when(FirebaseMessaging::getInstance).thenReturn(mockMessaging);
                given(mockMessaging.send(any(Message.class))).willReturn("projects/test/messages/12345");

                // when & then - 예외 없이 정상 종료
                fcmService.sendNotification("valid-token", "알림 제목", "알림 본문");

                verify(mockMessaging).send(any(Message.class));
            }
        }

        @Test
        @DisplayName("FirebaseMessagingException 발생 시 FCM_SEND_FAILED 예외")
        void throwsFcmSendFailed_onFirebaseException() throws FirebaseMessagingException {
            try (MockedStatic<FirebaseApp> mockedFirebaseApp = mockStatic(FirebaseApp.class);
                 MockedStatic<FirebaseMessaging> mockedMessaging = mockStatic(FirebaseMessaging.class)) {

                mockedFirebaseApp.when(FirebaseApp::getApps).thenReturn(List.of(org.mockito.Mockito.mock(FirebaseApp.class)));
                FirebaseMessaging mockMessaging = org.mockito.Mockito.mock(FirebaseMessaging.class);
                mockedMessaging.when(FirebaseMessaging::getInstance).thenReturn(mockMessaging);

                FirebaseMessagingException exception = org.mockito.Mockito.mock(FirebaseMessagingException.class);
                given(mockMessaging.send(any(Message.class))).willThrow(exception);

                // when & then
                assertThatThrownBy(() -> fcmService.sendNotification("invalid-token", "제목", "본문"))
                        .isInstanceOf(BusinessException.class)
                        .extracting(e -> ((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.FCM_SEND_FAILED);
            }
        }

        @Test
        @DisplayName("FCM_SEND_FAILED 에러코드 검증 - N001, 500")
        void fcmSendFailedErrorCode() {
            assertThat(ErrorCode.FCM_SEND_FAILED.getCode()).isEqualTo("N001");
            assertThat(ErrorCode.FCM_SEND_FAILED.getStatus())
                    .isEqualTo(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(ErrorCode.FCM_SEND_FAILED.getMessage()).isEqualTo("푸시 알림 전송에 실패했습니다");
        }
    }
}
