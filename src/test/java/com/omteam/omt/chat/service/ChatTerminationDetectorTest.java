package com.omteam.omt.chat.service;

import static org.assertj.core.api.Assertions.*;

import com.omteam.omt.chat.domain.ChatInputType;
import com.omteam.omt.chat.dto.ChatMessageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ChatTerminationDetectorTest {

    ChatTerminationDetector detector;

    @BeforeEach
    void setUp() {
        detector = new ChatTerminationDetector();
    }

    @Nested
    @DisplayName("null/시작 요청 처리")
    class NullAndStartRequestTests {

        @Test
        @DisplayName("null 요청이면 false 반환")
        void detectTerminationIntent_null_request_returns_false() {
            assertThat(detector.detectTerminationIntent(null)).isFalse();
        }

        @Test
        @DisplayName("채팅 시작 요청이면 false 반환")
        void detectTerminationIntent_start_request_returns_false() {
            ChatMessageRequest startRequest = ChatMessageRequest.builder().build();
            assertThat(detector.detectTerminationIntent(startRequest)).isFalse();
        }
    }

    @Nested
    @DisplayName("텍스트 입력 종료 감지")
    class TextInputTests {

        @ParameterizedTest
        @ValueSource(strings = {"종료", "끝", "그만", "나갈게", "안녕", "잘가", "바이"})
        @DisplayName("한국어 종료 키워드 감지")
        void detectTerminationIntent_korean_keywords(String keyword) {
            ChatMessageRequest request = ChatMessageRequest.builder()
                    .type(ChatInputType.TEXT)
                    .value(keyword)
                    .build();

            assertThat(detector.detectTerminationIntent(request)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"bye", "exit", "quit", "end", "BYE", "EXIT", "QUIT", "END"})
        @DisplayName("영어 종료 키워드 감지 (대소문자 무시)")
        void detectTerminationIntent_english_keywords(String keyword) {
            ChatMessageRequest request = ChatMessageRequest.builder()
                    .type(ChatInputType.TEXT)
                    .value(keyword)
                    .build();

            assertThat(detector.detectTerminationIntent(request)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "대화 종료해줘",
                "대화를 종료할게",
                "그만 할게",
                "그만할래",
                "끝낼게",
                "끝낼래",
                "이만 할게",
                "이만할래",
                "고마워",
                "정말 고마워요",
                "감사합니다",
                "도움 감사합니다"
        })
        @DisplayName("종료 패턴 감지")
        void detectTerminationIntent_patterns(String text) {
            ChatMessageRequest request = ChatMessageRequest.builder()
                    .type(ChatInputType.TEXT)
                    .value(text)
                    .build();

            assertThat(detector.detectTerminationIntent(request)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "운동이 힘들어요",
                "오늘 미션 추천해줘",
                "다리 스트레칭 방법 알려줘",
                "잘 모르겠어요",
                "도움이 필요해요"
        })
        @DisplayName("일반 대화는 종료 의도로 감지하지 않음")
        void detectTerminationIntent_normal_conversation_returns_false(String text) {
            ChatMessageRequest request = ChatMessageRequest.builder()
                    .type(ChatInputType.TEXT)
                    .value(text)
                    .build();

            assertThat(detector.detectTerminationIntent(request)).isFalse();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("빈 텍스트는 false 반환")
        void detectTerminationIntent_empty_text_returns_false(String text) {
            ChatMessageRequest request = ChatMessageRequest.builder()
                    .type(ChatInputType.TEXT)
                    .value(text)
                    .build();

            assertThat(detector.detectTerminationIntent(request)).isFalse();
        }

        @Test
        @DisplayName("키워드 앞뒤 공백 제거 후 감지")
        void detectTerminationIntent_trims_whitespace() {
            ChatMessageRequest request = ChatMessageRequest.builder()
                    .type(ChatInputType.TEXT)
                    .value("  종료  ")
                    .build();

            assertThat(detector.detectTerminationIntent(request)).isTrue();
        }
    }

    @Nested
    @DisplayName("옵션 선택 종료 감지")
    class OptionInputTests {

        @ParameterizedTest
        @ValueSource(strings = {"END", "EXIT", "QUIT", "TERMINATE", "CLOSE", "DONE"})
        @DisplayName("영어 종료 옵션 값 감지")
        void detectTerminationIntent_english_option_values(String value) {
            ChatMessageRequest request = ChatMessageRequest.builder()
                    .type(ChatInputType.OPTION)
                    .value(value)
                    .build();

            assertThat(detector.detectTerminationIntent(request)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"end", "exit", "quit", "terminate", "close", "done"})
        @DisplayName("영어 종료 옵션 값 감지 (소문자)")
        void detectTerminationIntent_english_option_values_lowercase(String value) {
            ChatMessageRequest request = ChatMessageRequest.builder()
                    .type(ChatInputType.OPTION)
                    .value(value)
                    .build();

            assertThat(detector.detectTerminationIntent(request)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"종료", "끝", "대화종료", "채팅종료"})
        @DisplayName("한국어 종료 옵션 값 감지")
        void detectTerminationIntent_korean_option_values(String value) {
            ChatMessageRequest request = ChatMessageRequest.builder()
                    .type(ChatInputType.OPTION)
                    .value(value)
                    .build();

            assertThat(detector.detectTerminationIntent(request)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"TIME_SHORTAGE", "MOTIVATION_LACK", "CONTINUE", "YES", "NO"})
        @DisplayName("일반 옵션 값은 종료 의도로 감지하지 않음")
        void detectTerminationIntent_normal_option_returns_false(String value) {
            ChatMessageRequest request = ChatMessageRequest.builder()
                    .type(ChatInputType.OPTION)
                    .value(value)
                    .build();

            assertThat(detector.detectTerminationIntent(request)).isFalse();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("빈 옵션 값은 false 반환")
        void detectTerminationIntent_empty_value_returns_false(String value) {
            ChatMessageRequest request = ChatMessageRequest.builder()
                    .type(ChatInputType.OPTION)
                    .value(value)
                    .build();

            assertThat(detector.detectTerminationIntent(request)).isFalse();
        }
    }
}
