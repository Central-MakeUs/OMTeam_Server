package com.omteam.omt.integration.mission;

import static org.assertj.core.api.Assertions.*;

import com.omteam.omt.common.exception.BusinessException;
import com.omteam.omt.common.exception.ErrorCode;
import com.omteam.omt.integration.IntegrationTestBase;
import com.omteam.omt.mission.client.AiMissionClient;
import com.omteam.omt.mission.client.dto.AiMissionRecommendRequest;
import com.omteam.omt.mission.client.dto.AiMissionRecommendRequest.OnboardingData;
import com.omteam.omt.mission.client.dto.AiMissionRecommendResponse;
import com.omteam.omt.mission.domain.MissionType;
import com.omteam.omt.user.domain.LifestyleType;
import com.omteam.omt.user.domain.WorkTimeType;
import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@DisplayName("[통합] AiMissionClient - AI 서버 미션 추천 API 호출")
class AiMissionClientIntegrationTest extends IntegrationTestBase {

    @Autowired
    private AiMissionClient aiMissionClient;

    @Test
    @DisplayName("AI 서버로부터 미션 추천 응답을 정상적으로 받아서 바인딩한다")
    void recommendDailyMissions_success() throws Exception {
        // given
        String responseBody = """
            {
                "missions": [
                    {
                        "name": "스트레칭 10분",
                        "type": "EXERCISE",
                        "difficulty": 1,
                        "estimatedMinutes": 10,
                        "estimatedCalories": 30
                    },
                    {
                        "name": "계단 오르기 5층",
                        "type": "EXERCISE",
                        "difficulty": 2,
                        "estimatedMinutes": 15,
                        "estimatedCalories": 50
                    },
                    {
                        "name": "점심 샐러드 먹기",
                        "type": "DIET",
                        "difficulty": 2,
                        "estimatedMinutes": 30,
                        "estimatedCalories": 0
                    }
                ]
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(responseBody));

        AiMissionRecommendRequest request = createTestRequest();

        // when
        AiMissionRecommendResponse response = aiMissionClient.recommendDailyMissions(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getMissions()).hasSize(3);

        // 첫 번째 미션 검증
        var mission1 = response.getMissions().get(0);
        assertThat(mission1.getName()).isEqualTo("스트레칭 10분");
        assertThat(mission1.getType()).isEqualTo(MissionType.EXERCISE);
        assertThat(mission1.getDifficulty()).isEqualTo(1);
        assertThat(mission1.getEstimatedMinutes()).isEqualTo(10);
        assertThat(mission1.getEstimatedCalories()).isEqualTo(30);

        // 두 번째 미션 검증
        var mission2 = response.getMissions().get(1);
        assertThat(mission2.getName()).isEqualTo("계단 오르기 5층");
        assertThat(mission2.getType()).isEqualTo(MissionType.EXERCISE);
        assertThat(mission2.getDifficulty()).isEqualTo(2);

        // 세 번째 미션 검증 (DIET 타입)
        var mission3 = response.getMissions().get(2);
        assertThat(mission3.getName()).isEqualTo("점심 샐러드 먹기");
        assertThat(mission3.getType()).isEqualTo(MissionType.DIET);
    }

    @Test
    @DisplayName("AI 서버가 500 에러를 반환하면 AI_SERVER_ERROR 예외가 발생한다")
    void recommendDailyMissions_serverError() {
        // given
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"error\": \"Internal Server Error\"}"));

        AiMissionRecommendRequest request = createTestRequest();

        // when & then
        assertThatThrownBy(() -> aiMissionClient.recommendDailyMissions(request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AI_SERVER_ERROR);
    }

    @Test
    @DisplayName("AI 서버가 400 에러를 반환하면 AI_SERVER_ERROR 예외가 발생한다")
    void recommendDailyMissions_badRequest() {
        // given
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(400)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"error\": \"Bad Request\"}"));

        AiMissionRecommendRequest request = createTestRequest();

        // when & then
        assertThatThrownBy(() -> aiMissionClient.recommendDailyMissions(request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AI_SERVER_ERROR);
    }

    @Test
    @DisplayName("응답의 미션 데이터를 Mission 엔티티로 변환할 수 있다")
    void recommendDailyMissions_canConvertToEntity() throws Exception {
        // given
        String responseBody = """
            {
                "missions": [
                    {
                        "name": "요가 20분",
                        "type": "EXERCISE",
                        "difficulty": 3,
                        "estimatedMinutes": 20,
                        "estimatedCalories": 80
                    }
                ]
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(responseBody));

        AiMissionRecommendRequest request = createTestRequest();

        // when
        AiMissionRecommendResponse response = aiMissionClient.recommendDailyMissions(request);
        var missionEntity = response.getMissions().get(0).toEntity();

        // then
        assertThat(missionEntity.getName()).isEqualTo("요가 20분");
        assertThat(missionEntity.getType()).isEqualTo(MissionType.EXERCISE);
        assertThat(missionEntity.getDifficulty()).isEqualTo(3);
        assertThat(missionEntity.getEstimatedMinutes()).isEqualTo(20);
        assertThat(missionEntity.getEstimatedCalories()).isEqualTo(80);
    }

    @Test
    @DisplayName("빈 미션 목록이 반환되어도 정상 처리된다")
    void recommendDailyMissions_emptyMissions() throws Exception {
        // given
        String responseBody = """
            {
                "missions": []
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(responseBody));

        AiMissionRecommendRequest request = createTestRequest();

        // when
        AiMissionRecommendResponse response = aiMissionClient.recommendDailyMissions(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getMissions()).isEmpty();
    }

    @Test
    @DisplayName("AI 서버 응답이 타임아웃되면 AI_SERVER_CONNECTION_ERROR 예외가 발생한다")
    void recommendDailyMissions_timeout() {
        // given - 타임아웃(5초)보다 긴 지연 설정
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"missions\": []}")
                .setBodyDelay(7, TimeUnit.SECONDS));

        AiMissionRecommendRequest request = createTestRequest();

        // when & then
        assertThatThrownBy(() -> aiMissionClient.recommendDailyMissions(request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AI_SERVER_CONNECTION_ERROR);
    }

    @Test
    @DisplayName("AI 서버가 잘못된 JSON 응답을 반환하면 예외가 발생한다")
    void recommendDailyMissions_malformedResponse() {
        // given
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("this is not valid json"));

        AiMissionRecommendRequest request = createTestRequest();

        // when & then
        assertThatThrownBy(() -> aiMissionClient.recommendDailyMissions(request))
                .isInstanceOf(BusinessException.class);
    }

    private AiMissionRecommendRequest createTestRequest() {
        return AiMissionRecommendRequest.builder()
                .userId(1L)
                .onboarding(OnboardingData.builder()
                        .appGoal("체중 감량")
                        .workTimeType(WorkTimeType.FIXED)
                        .availableStartTime("18:00")
                        .availableEndTime("21:00")
                        .minExerciseMinutes(30)
                        .preferredExercises(List.of("스트레칭", "걷기"))
                        .lifestyleType(LifestyleType.REGULAR_DAYTIME)
                        .build())
                .recentMissionHistory(Collections.emptyList())
                .weeklyFailureReasons(Collections.emptyList())
                .build();
    }
}
