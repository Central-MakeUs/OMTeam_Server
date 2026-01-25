package com.omteam.omt.character.client;

import com.omteam.omt.character.client.dto.AiDailyAnalysisRequest;
import com.omteam.omt.character.client.dto.AiDailyAnalysisResponse;
import com.omteam.omt.common.exception.BusinessException;
import com.omteam.omt.common.exception.ErrorCode;
import com.omteam.omt.config.properties.AiServerProperties;
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
public class AiDailyAnalysisClient {

    private final WebClient webClient;
    private final AiServerProperties aiServerProperties;

    private static final String DAILY_ANALYSIS_ENDPOINT = "/ai/analysis/daily";

    public AiDailyAnalysisResponse requestDailyAnalysis(AiDailyAnalysisRequest request) {
        try {
            return webClient.post()
                    .uri(aiServerProperties.getBaseUrl() + DAILY_ANALYSIS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(AiDailyAnalysisResponse.class)
                    .timeout(Duration.ofSeconds(aiServerProperties.getTimeoutSeconds()))
                    .block();
        } catch (WebClientResponseException e) {
            log.error("AI 서버 응답 오류: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.AI_SERVER_ERROR);
        } catch (Exception e) {
            log.error("AI 서버 통신 오류", e);
            throw new BusinessException(ErrorCode.AI_SERVER_CONNECTION_ERROR);
        }
    }
}
