package com.omteam.omt.mission.client;

import com.omteam.omt.common.exception.BusinessException;
import com.omteam.omt.common.exception.ErrorCode;
import com.omteam.omt.config.properties.AiServerProperties;
import com.omteam.omt.mission.client.dto.AiMissionRecommendRequest;
import com.omteam.omt.mission.client.dto.AiMissionRecommendResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiMissionClient {

    private final WebClient webClient;
    private final AiServerProperties aiServerProperties;
    private final CircuitBreaker aiServerCircuitBreaker;

    private static final String DAILY_MISSION_ENDPOINT = "/ai/missions/daily";

    public AiMissionRecommendResponse recommendDailyMissions(AiMissionRecommendRequest request) {
        try {
            return aiServerCircuitBreaker.executeSupplier(() -> {
                try {
                    return webClient.post()
                            .uri(aiServerProperties.getBaseUrl() + DAILY_MISSION_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(request)
                            .retrieve()
                            .bodyToMono(AiMissionRecommendResponse.class)
                            .timeout(Duration.ofSeconds(aiServerProperties.getTimeoutSeconds()))
                            .block();
                } catch (WebClientResponseException e) {
                    log.error("AI 서버 응답 오류: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
                    throw e;
                } catch (Exception e) {
                    log.error("AI 서버 통신 오류", e);
                    throw e;
                }
            });
        } catch (CallNotPermittedException e) {
            log.warn("AI 서버 Circuit Breaker OPEN 상태 - 미션 추천 차단");
            throw new BusinessException(ErrorCode.AI_SERVER_CIRCUIT_OPEN);
        } catch (WebClientResponseException e) {
            throw new BusinessException(ErrorCode.AI_SERVER_ERROR);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 서버 미션 추천 실패", e);
            throw new BusinessException(ErrorCode.AI_SERVER_CONNECTION_ERROR);
        }
    }
}
