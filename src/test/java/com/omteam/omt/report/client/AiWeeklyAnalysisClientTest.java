package com.omteam.omt.report.client;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import com.omteam.omt.common.ai.dto.UserContext;
import com.omteam.omt.common.exception.BusinessException;
import com.omteam.omt.common.exception.ErrorCode;
import com.omteam.omt.config.properties.AiServerProperties;
import com.omteam.omt.report.client.dto.AiWeeklyAnalysisRequest;
import com.omteam.omt.report.client.dto.AiWeeklyAnalysisResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class AiWeeklyAnalysisClientTest {

    @Mock
    WebClient webClient;

    @Mock
    WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    WebClient.RequestBodySpec requestBodySpec;

    @Mock
    WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    WebClient.ResponseSpec responseSpec;

    @Mock
    AiServerProperties aiServerProperties;

    AiWeeklyAnalysisClient aiWeeklyAnalysisClient;

    @BeforeEach
    void setUp() {
        aiWeeklyAnalysisClient = new AiWeeklyAnalysisClient(webClient, aiServerProperties);
    }

    @Test
    @DisplayName("AI 서버 호출 성공 시 응답 반환")
    void analyzeWeeklyMissions_success() {
        // given
        UserContext mockUserContext = UserContext.builder()
                .nickname("테스트 사용자")
                .appGoal("건강 증진")
                .currentLevel(1)
                .build();

        AiWeeklyAnalysisRequest request = AiWeeklyAnalysisRequest.of(
                1L,
                mockUserContext,
                LocalDate.of(2024, 1, 15),
                LocalDate.of(2024, 1, 21),
                List.of("시간 부족", "피로"),
                List.of(),
                List.of()
        );

        AiWeeklyAnalysisResponse expectedResponse = new AiWeeklyAnalysisResponse();
        expectedResponse.setMainFailureReason("시간 부족");
        expectedResponse.setOverallFeedback("이번 주 피드백입니다.");

        given(aiServerProperties.getBaseUrl()).willReturn("http://localhost:8000");
        given(aiServerProperties.getTimeoutSeconds()).willReturn(30);

        given(webClient.post()).willReturn(requestBodyUriSpec);
        given(requestBodyUriSpec.uri("http://localhost:8000/ai/analysis/weekly")).willReturn(requestBodySpec);
        given(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).willReturn(requestBodySpec);
        given(requestBodySpec.bodyValue(request)).willReturn(requestHeadersSpec);
        given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.bodyToMono(AiWeeklyAnalysisResponse.class))
                .willReturn(Mono.just(expectedResponse));

        // when
        AiWeeklyAnalysisResponse response = aiWeeklyAnalysisClient.analyzeWeeklyMissions(request);

        // then
        assertThat(response.getMainFailureReason()).isEqualTo("시간 부족");
        assertThat(response.getOverallFeedback()).isEqualTo("이번 주 피드백입니다.");
    }

    @Test
    @DisplayName("AI 서버 응답 오류 시 AI_SERVER_ERROR 예외 발생")
    void analyzeWeeklyMissions_serverError() {
        // given
        UserContext mockUserContext = UserContext.builder()
                .nickname("테스트 사용자")
                .appGoal("건강 증진")
                .currentLevel(1)
                .build();

        AiWeeklyAnalysisRequest request = AiWeeklyAnalysisRequest.of(
                1L,
                mockUserContext,
                LocalDate.of(2024, 1, 15),
                LocalDate.of(2024, 1, 21),
                List.of(),
                List.of(),
                List.of()
        );

        given(aiServerProperties.getBaseUrl()).willReturn("http://localhost:8000");
        given(aiServerProperties.getTimeoutSeconds()).willReturn(30);

        given(webClient.post()).willReturn(requestBodyUriSpec);
        given(requestBodyUriSpec.uri("http://localhost:8000/ai/analysis/weekly")).willReturn(requestBodySpec);
        given(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).willReturn(requestBodySpec);
        given(requestBodySpec.bodyValue(request)).willReturn(requestHeadersSpec);
        given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.bodyToMono(AiWeeklyAnalysisResponse.class))
                .willReturn(Mono.error(WebClientResponseException.create(500, "Internal Server Error", null, null, null)));

        // when & then
        assertThatThrownBy(() -> aiWeeklyAnalysisClient.analyzeWeeklyMissions(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AI_SERVER_ERROR);
    }

    @Test
    @DisplayName("AI 서버 연결 오류 시 AI_SERVER_CONNECTION_ERROR 예외 발생")
    void analyzeWeeklyMissions_connectionError() {
        // given
        UserContext mockUserContext = UserContext.builder()
                .nickname("테스트 사용자")
                .appGoal("건강 증진")
                .currentLevel(1)
                .build();

        AiWeeklyAnalysisRequest request = AiWeeklyAnalysisRequest.of(
                1L,
                mockUserContext,
                LocalDate.of(2024, 1, 15),
                LocalDate.of(2024, 1, 21),
                List.of(),
                List.of(),
                List.of()
        );

        given(aiServerProperties.getBaseUrl()).willReturn("http://localhost:8000");
        given(aiServerProperties.getTimeoutSeconds()).willReturn(30);

        given(webClient.post()).willReturn(requestBodyUriSpec);
        given(requestBodyUriSpec.uri("http://localhost:8000/ai/analysis/weekly")).willReturn(requestBodySpec);
        given(requestBodySpec.contentType(MediaType.APPLICATION_JSON)).willReturn(requestBodySpec);
        given(requestBodySpec.bodyValue(request)).willReturn(requestHeadersSpec);
        given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.bodyToMono(AiWeeklyAnalysisResponse.class))
                .willReturn(Mono.error(new RuntimeException("Connection refused")));

        // when & then
        assertThatThrownBy(() -> aiWeeklyAnalysisClient.analyzeWeeklyMissions(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AI_SERVER_CONNECTION_ERROR);
    }
}
