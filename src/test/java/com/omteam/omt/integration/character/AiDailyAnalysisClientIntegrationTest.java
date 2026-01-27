package com.omteam.omt.integration.character;

import static org.assertj.core.api.Assertions.*;

import com.omteam.omt.character.client.AiDailyAnalysisClient;
import com.omteam.omt.character.client.dto.AiDailyAnalysisRequest;
import com.omteam.omt.character.client.dto.AiDailyAnalysisResponse;
import com.omteam.omt.character.domain.EncouragementIntent;
import com.omteam.omt.common.exception.BusinessException;
import com.omteam.omt.common.exception.ErrorCode;
import com.omteam.omt.integration.IntegrationTestBase;
import com.omteam.omt.mission.domain.MissionResult;
import com.omteam.omt.mission.domain.MissionType;
import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@DisplayName("[통합] AiDailyAnalysisClient - AI 서버 일일 분석 API 호출")
class AiDailyAnalysisClientIntegrationTest extends IntegrationTestBase {

    @Autowired
    private AiDailyAnalysisClient aiDailyAnalysisClient;

    @Test
    @DisplayName("AI 서버로부터 일일 분석 응답을 정상적으로 받아서 바인딩한다")
    void requestDailyAnalysis_success() throws Exception {
        // given
        String responseBody = """
            {
                "feedbackText": "오늘도 꾸준히 운동을 완료했네요! 이대로 가면 목표 달성이 가능해요.",
                "encouragementCandidates": [
                    {
                        "intent": "PRAISE",
                        "title": "대단해요!",
                        "message": "연속 3일째 미션 성공이에요. 이 기세를 유지해봐요!"
                    },
                    {
                        "intent": "RETRY",
                        "title": "다시 시작해봐요",
                        "message": "어제는 쉬었지만 오늘은 다시 시작할 수 있어요."
                    },
                    {
                        "intent": "NORMAL",
                        "title": "꾸준함이 중요해요",
                        "message": "작은 습관이 큰 변화를 만들어요."
                    },
                    {
                        "intent": "PUSH",
                        "title": "오늘 시작해볼까요?",
                        "message": "아직 오늘의 미션을 시작하지 않았어요. 지금 바로 시작해봐요!"
                    }
                ]
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(responseBody));

        AiDailyAnalysisRequest request = createTestRequest();

        // when
        AiDailyAnalysisResponse response = aiDailyAnalysisClient.requestDailyAnalysis(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getFeedbackText()).contains("꾸준히 운동을 완료");
        assertThat(response.getEncouragementCandidates()).hasSize(4);

        // PRAISE 메시지 검증
        var praise = response.getEncouragementOf(EncouragementIntent.PRAISE);
        assertThat(praise).isNotNull();
        assertThat(praise.getIntent()).isEqualTo(EncouragementIntent.PRAISE);
        assertThat(praise.getTitle()).isEqualTo("대단해요!");
        assertThat(praise.getMessage()).contains("연속 3일째");

        // RETRY 메시지 검증
        var retry = response.getEncouragementOf(EncouragementIntent.RETRY);
        assertThat(retry).isNotNull();
        assertThat(retry.getIntent()).isEqualTo(EncouragementIntent.RETRY);
        assertThat(retry.getTitle()).isEqualTo("다시 시작해봐요");

        // NORMAL 메시지 검증
        var normal = response.getEncouragementOf(EncouragementIntent.NORMAL);
        assertThat(normal).isNotNull();
        assertThat(normal.getIntent()).isEqualTo(EncouragementIntent.NORMAL);

        // PUSH 메시지 검증
        var push = response.getEncouragementOf(EncouragementIntent.PUSH);
        assertThat(push).isNotNull();
        assertThat(push.getIntent()).isEqualTo(EncouragementIntent.PUSH);
    }

    @Test
    @DisplayName("미션 수행 결과 없이 요청해도 정상 응답을 받는다")
    void requestDailyAnalysis_withoutYesterdayMission() throws Exception {
        // given
        String responseBody = """
            {
                "feedbackText": "아직 미션을 시작하지 않았네요. 오늘 첫 미션을 시작해보는 건 어떨까요?",
                "encouragementCandidates": [
                    {
                        "intent": "PUSH",
                        "title": "시작이 반이에요",
                        "message": "작은 미션부터 시작해보세요!"
                    }
                ]
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(responseBody));

        AiDailyAnalysisRequest request = AiDailyAnalysisRequest.builder()
                .userId(1L)
                .targetDate("2024-01-15")
                .todayMission(null)
                .build();

        // when
        AiDailyAnalysisResponse response = aiDailyAnalysisClient.requestDailyAnalysis(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getFeedbackText()).contains("아직 미션을 시작하지 않았네요");
        assertThat(response.getEncouragementCandidates()).isNotEmpty();
    }

    @Test
    @DisplayName("AI 서버가 500 에러를 반환하면 AI_SERVER_ERROR 예외가 발생한다")
    void requestDailyAnalysis_serverError() {
        // given
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"error\": \"Internal Server Error\"}"));

        AiDailyAnalysisRequest request = createTestRequest();

        // when & then
        assertThatThrownBy(() -> aiDailyAnalysisClient.requestDailyAnalysis(request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AI_SERVER_ERROR);
    }

    @Test
    @DisplayName("일부 intent만 포함된 응답도 정상 처리된다")
    void requestDailyAnalysis_partialIntents() throws Exception {
        // given
        String responseBody = """
            {
                "feedbackText": "미션 실패를 경험했네요. 괜찮아요, 다시 도전해봐요!",
                "encouragementCandidates": [
                    {
                        "intent": "RETRY",
                        "title": "다시 도전해요",
                        "message": "실패는 성공의 어머니예요."
                    }
                ]
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(responseBody));

        AiDailyAnalysisRequest request = createTestRequest();

        // when
        AiDailyAnalysisResponse response = aiDailyAnalysisClient.requestDailyAnalysis(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getEncouragementOf(EncouragementIntent.RETRY)).isNotNull();
        assertThat(response.getEncouragementOf(EncouragementIntent.PRAISE)).isNull();
        assertThat(response.getEncouragementOf(EncouragementIntent.NORMAL)).isNull();
        assertThat(response.getEncouragementOf(EncouragementIntent.PUSH)).isNull();
    }

    @Test
    @DisplayName("실패 사유와 함께 요청하면 맞춤 피드백을 받는다")
    void requestDailyAnalysis_withFailureReason() throws Exception {
        // given
        String responseBody = """
            {
                "feedbackText": "시간 부족으로 미션을 완료하지 못했군요. 다음에는 더 짧은 미션을 선택해보는 건 어떨까요?",
                "encouragementCandidates": [
                    {
                        "intent": "RETRY",
                        "title": "작은 목표부터",
                        "message": "10분 미션으로 다시 시작해봐요."
                    }
                ]
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(responseBody));

        AiDailyAnalysisRequest request = AiDailyAnalysisRequest.builder()
                .userId(1L)
                .targetDate("2024-01-15")
                .todayMission(AiDailyAnalysisRequest.TodayMission.builder()
                        .missionType(MissionType.EXERCISE)
                        .difficulty(3)
                        .result(MissionResult.FAILURE)
                        .failureReason("시간 부족")
                        .build())
                .build();

        // when
        AiDailyAnalysisResponse response = aiDailyAnalysisClient.requestDailyAnalysis(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getFeedbackText()).contains("시간 부족");
    }

    private AiDailyAnalysisRequest createTestRequest() {
        return AiDailyAnalysisRequest.builder()
                .userId(1L)
                .targetDate("2024-01-15")
                .todayMission(AiDailyAnalysisRequest.TodayMission.builder()
                        .missionType(MissionType.EXERCISE)
                        .difficulty(2)
                        .result(MissionResult.SUCCESS)
                        .failureReason(null)
                        .build())
                .build();
    }
}
