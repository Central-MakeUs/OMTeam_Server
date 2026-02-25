package com.omteam.omt.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.omteam.omt.common.exception.BusinessException;
import com.omteam.omt.common.exception.ErrorCode;
import com.omteam.omt.user.domain.User;
import com.omteam.omt.user.service.UserQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("[단위] NotificationService")
class NotificationServiceTest {

    @Mock
    UserQueryService userQueryService;

    @InjectMocks
    NotificationService notificationService;

    final Long userId = 1L;

    @Nested
    @DisplayName("registerFcmToken - FCM 토큰 등록/갱신")
    class RegisterFcmToken {

        @Test
        @DisplayName("성공 - FCM 토큰이 사용자에게 저장된다")
        void success() {
            // given
            User user = createUser();
            given(userQueryService.getUser(userId)).willReturn(user);

            // when
            notificationService.registerFcmToken(userId, "new-fcm-token");

            // then
            assertThat(user.getFcmToken()).isEqualTo("new-fcm-token");
            then(userQueryService).should().getUser(userId);
        }

        @Test
        @DisplayName("성공 - 기존 토큰이 있어도 새 토큰으로 갱신된다")
        void success_updateExistingToken() {
            // given
            User user = createUserWithFcmToken("old-token");
            given(userQueryService.getUser(userId)).willReturn(user);

            // when
            notificationService.registerFcmToken(userId, "new-token");

            // then
            assertThat(user.getFcmToken()).isEqualTo("new-token");
        }

        @Test
        @DisplayName("실패 - 사용자를 찾을 수 없으면 USER_NOT_FOUND 예외")
        void fail_userNotFound() {
            // given
            given(userQueryService.getUser(userId))
                    .willThrow(new BusinessException(ErrorCode.USER_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> notificationService.registerFcmToken(userId, "token"))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("deleteFcmToken - FCM 토큰 삭제")
    class DeleteFcmToken {

        @Test
        @DisplayName("성공 - FCM 토큰이 null로 초기화된다")
        void success() {
            // given
            User user = createUserWithFcmToken("existing-token");
            given(userQueryService.getUser(userId)).willReturn(user);

            // when
            notificationService.deleteFcmToken(userId);

            // then
            assertThat(user.getFcmToken()).isNull();
            then(userQueryService).should().getUser(userId);
        }

        @Test
        @DisplayName("성공 - 토큰이 없어도 예외 없이 null로 설정된다")
        void success_noExistingToken() {
            // given
            User user = createUser();
            given(userQueryService.getUser(userId)).willReturn(user);

            // when
            notificationService.deleteFcmToken(userId);

            // then
            assertThat(user.getFcmToken()).isNull();
        }

        @Test
        @DisplayName("실패 - 사용자를 찾을 수 없으면 USER_NOT_FOUND 예외")
        void fail_userNotFound() {
            // given
            given(userQueryService.getUser(userId))
                    .willThrow(new BusinessException(ErrorCode.USER_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> notificationService.deleteFcmToken(userId))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }
    }

    /* ======================== */
    /* ===== Helper Zone ====== */
    /* ======================== */

    private User createUser() {
        return User.builder()
                .userId(userId)
                .email("test@example.com")
                .build();
    }

    private User createUserWithFcmToken(String fcmToken) {
        return User.builder()
                .userId(userId)
                .email("test@example.com")
                .fcmToken(fcmToken)
                .build();
    }
}
