package com.omteam.omt.integration.chat;

import static org.assertj.core.api.Assertions.*;

import com.omteam.omt.chat.client.AiChatClient;
import com.omteam.omt.chat.client.dto.AiChatRequest;
import com.omteam.omt.chat.client.dto.AiChatResponse;
import com.omteam.omt.chat.domain.ChatInputType;
import com.omteam.omt.common.ai.dto.UserContext;
import com.omteam.omt.common.exception.BusinessException;
import com.omteam.omt.common.exception.ErrorCode;
import com.omteam.omt.integration.IntegrationTestBase;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@DisplayName("[통합] AiChatClient - AI 서버 채팅 API 호출")
class AiChatClientIntegrationTest extends IntegrationTestBase {

    @Autowired
    private AiChatClient aiChatClient;

    @Test
    @DisplayName("AI 서버로부터 채팅 응답을 정상적으로 받아서 바인딩한다")
    void sendMessage_success() {
        // given
        String responseBody = """
            {
                "botMessage": {
                    "text": "안녕하세요! 무엇을 도와드릴까요?",
                    "options": [
                        { "label": "운동이 힘들어요", "value": "EXERCISE_HARD" },
                        { "label": "동기부여가 필요해요", "value": "NEED_MOTIVATION" },
                        { "label": "다른 고민이 있어요", "value": "OTHER" }
                    ]
                },
                "state": { "isTerminal": false }
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(responseBody));

        AiChatRequest request = createTestRequest(null);  // 채팅 시작

        // when
        AiChatResponse response = aiChatClient.sendMessage(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getBotMessage()).isNotNull();
        assertThat(response.getBotMessage().getText()).contains("안녕하세요");
        assertThat(response.getBotMessage().getOptions()).hasSize(3);
        assertThat(response.isTerminal()).isFalse();
    }

    @Test
    @DisplayName("텍스트 입력에 대한 응답을 정상적으로 받는다")
    void sendMessage_textInput() {
        // given
        String responseBody = """
            {
                "botMessage": {
                    "text": "운동이 힘드시군요. 어떤 점이 가장 어려우신가요?",
                    "options": [
                        { "label": "시간이 부족해요", "value": "TIME_SHORTAGE" },
                        { "label": "체력이 부족해요", "value": "STAMINA_SHORTAGE" }
                    ]
                },
                "state": { "isTerminal": false }
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(responseBody));

        AiChatRequest.Input input = AiChatRequest.Input.builder()
                .type(ChatInputType.TEXT)
                .text("운동이 너무 힘들어요")
                .build();

        AiChatRequest request = createTestRequest(input);

        // when
        AiChatResponse response = aiChatClient.sendMessage(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getBotMessage().getText()).contains("운동이 힘드시군요");
        assertThat(response.getBotMessage().getOptions()).hasSize(2);
    }

    @Test
    @DisplayName("선택지 선택에 대한 응답을 정상적으로 받는다")
    void sendMessage_optionInput() {
        // given
        String responseBody = """
            {
                "botMessage": {
                    "text": "시간이 부족하시군요. 짧은 시간에 할 수 있는 운동을 추천드릴게요.",
                    "options": []
                },
                "state": { "isTerminal": false }
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(responseBody));

        AiChatRequest.Input input = AiChatRequest.Input.builder()
                .type(ChatInputType.OPTION)
                .value("TIME_SHORTAGE")
                .build();

        AiChatRequest request = createTestRequest(input);

        // when
        AiChatResponse response = aiChatClient.sendMessage(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getBotMessage().getText()).contains("시간이 부족하시군요");
    }

    @Test
    @DisplayName("대화 종료 응답을 정상적으로 처리한다")
    void sendMessage_terminalResponse() {
        // given
        String responseBody = """
            {
                "botMessage": {
                    "text": "좋은 대화였어요! 언제든 다시 찾아주세요.",
                    "options": []
                },
                "state": { "isTerminal": true }
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(responseBody));

        AiChatRequest request = createTestRequest(null);

        // when
        AiChatResponse response = aiChatClient.sendMessage(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.isTerminal()).isTrue();
    }

    @Test
    @DisplayName("AI 서버가 500 에러를 반환하면 AI_SERVER_ERROR 예외가 발생한다")
    void sendMessage_serverError() {
        // given
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"error\": \"Internal Server Error\"}"));

        AiChatRequest request = createTestRequest(null);

        // when & then
        assertThatThrownBy(() -> aiChatClient.sendMessage(request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AI_SERVER_ERROR);
    }

    @Test
    @DisplayName("선택지가 없는 응답도 정상 처리된다")
    void sendMessage_noOptions() {
        // given
        String responseBody = """
            {
                "botMessage": {
                    "text": "자유롭게 말씀해주세요.",
                    "options": []
                },
                "state": { "isTerminal": false }
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(responseBody));

        AiChatRequest request = createTestRequest(null);

        // when
        AiChatResponse response = aiChatClient.sendMessage(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getBotMessage().getOptions()).isEmpty();
    }

    private AiChatRequest createTestRequest(AiChatRequest.Input input) {
        return AiChatRequest.builder()
                .input(input)
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .userContext(UserContext.builder()
                        .nickname("테스트유저")
                        .appGoal("체중 감량")
                        .currentLevel(2)
                        .successCount(15)
                        .build())
                .conversationHistory(List.of())
                .build();
    }
}
