package com.omteam.omt.chat.client;

import com.omteam.omt.chat.client.dto.AiChatRequest;
import com.omteam.omt.chat.client.dto.AiChatResponse;
import com.omteam.omt.config.properties.AiServerProperties;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiChatClient {

    private final WebClient webClient;
    private final AiServerProperties aiServerProperties;
    private final CircuitBreaker aiServerCircuitBreaker;

    private static final String CHAT_ENDPOINT = "/ai/chat/messages";

    public AiChatResponse sendMessage(AiChatRequest request) {
        try {
            return aiServerCircuitBreaker.executeSupplier(() -> {
                log.debug("AI 채팅 요청: input={}", request.getInput());
                return webClient.post()
                        .uri(aiServerProperties.getBaseUrl() + CHAT_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(AiChatResponse.class)
                        .timeout(Duration.ofSeconds(aiServerProperties.getTimeoutSeconds()))
                        .block();
            });
        } catch (CallNotPermittedException e) {
            log.warn("AI 서버 Circuit Breaker OPEN 상태 - 채팅 fallback 반환");
            return AiChatResponse.timeoutFallback();
        } catch (WebClientResponseException e) {
            log.warn("AI 서버 응답 오류 - 채팅: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            return AiChatResponse.timeoutFallback();
        } catch (WebClientRequestException e) {
            log.warn("AI 서버 연결 오류 - 채팅 fallback 반환", e);
            return AiChatResponse.timeoutFallback();
        } catch (Exception e) {
            if (isTimeoutException(e)) {
                log.warn("AI 서버 타임아웃 - 채팅 fallback 반환", e);
            } else {
                log.error("AI 서버 통신 중 예상치 못한 오류", e);
            }
            return AiChatResponse.timeoutFallback();
        }
    }

    private boolean isTimeoutException(Throwable e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof TimeoutException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
