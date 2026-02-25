package com.omteam.omt.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;

import com.google.firebase.FirebaseApp;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

@DisplayName("[단위] FcmConfig")
class FcmConfigTest {

    @Nested
    @DisplayName("initFirebase")
    class InitFirebase {

        @Test
        @DisplayName("serviceAccountPath가 null이면 초기화 스킵 - 예외 없음")
        void noopWhenPathIsNull() throws Exception {
            FcmConfig config = new FcmConfig();
            setField(config, "serviceAccountPath", null);

            assertThatNoException().isThrownBy(config::initFirebase);
        }

        @Test
        @DisplayName("serviceAccountPath가 빈 문자열이면 초기화 스킵 - 예외 없음")
        void noopWhenPathIsBlank() throws Exception {
            FcmConfig config = new FcmConfig();
            setField(config, "serviceAccountPath", "");

            assertThatNoException().isThrownBy(config::initFirebase);
        }

        @Test
        @DisplayName("serviceAccountPath가 공백이면 초기화 스킵 - 예외 없음")
        void noopWhenPathIsWhitespace() throws Exception {
            FcmConfig config = new FcmConfig();
            setField(config, "serviceAccountPath", "   ");

            assertThatNoException().isThrownBy(config::initFirebase);
        }

        @Test
        @DisplayName("Firebase가 이미 초기화된 상태면 재초기화 스킵")
        void skipWhenAlreadyInitialized() throws Exception {
            FcmConfig config = new FcmConfig();
            setField(config, "serviceAccountPath", "/some/path.json");

            try (MockedStatic<FirebaseApp> mockedFirebaseApp = mockStatic(FirebaseApp.class)) {
                mockedFirebaseApp.when(FirebaseApp::getApps)
                        .thenReturn(List.of(org.mockito.Mockito.mock(FirebaseApp.class)));

                // Firebase 이미 초기화 상태 - initFirebase 스킵해야 함
                // 파일이 없어도 예외 없이 리턴
                assertThatNoException().isThrownBy(config::initFirebase);
            }
        }

        @Test
        @DisplayName("존재하지 않는 파일 경로면 IllegalStateException 발생")
        void throwsWhenFileNotFound() throws Exception {
            FcmConfig config = new FcmConfig();
            setField(config, "serviceAccountPath", "/nonexistent/path/service-account.json");

            try (MockedStatic<FirebaseApp> mockedFirebaseApp = mockStatic(FirebaseApp.class)) {
                mockedFirebaseApp.when(FirebaseApp::getApps).thenReturn(Collections.emptyList());

                assertThatThrownBy(config::initFirebase)
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("Firebase initialization failed");
            }
        }
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
