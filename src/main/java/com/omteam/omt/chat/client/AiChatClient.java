package com.omteam.omt.chat.client;

import com.omteam.omt.chat.client.dto.AiChatRequest;
import com.omteam.omt.chat.client.dto.AiChatResponse;
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
public class AiChatClient {

    private final WebClient webClient;
    private final AiServerProperties aiServerProperties;

    private static final String CHAT_ENDPOINT = "/ai/chat";

    public AiChatResponse sendMessage(AiChatRequest request) {
        try {
            log.debug("AI 채팅 요청: input={}", request.getInput());

            return webClient.post()
                    .uri(aiServerProperties.getBaseUrl() + CHAT_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(AiChatResponse.class)
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
