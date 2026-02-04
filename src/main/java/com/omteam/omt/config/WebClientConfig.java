package com.omteam.omt.config;

import com.omteam.omt.config.properties.AiServerProperties;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.util.retry.Retry;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    private static final int CONNECT_TIMEOUT_MILLIS = 5000;
    private static final int MAX_CONNECTIONS = 50;
    private static final int MAX_IDLE_TIME_SECONDS = 30;
    private static final int MAX_LIFE_TIME_MINUTES = 5;
    private static final int PENDING_ACQUIRE_TIMEOUT_SECONDS = 60;
    private static final int EVICT_IN_BACKGROUND_SECONDS = 120;
    private static final int MAX_RETRY_ATTEMPTS = 2;
    private static final long RETRY_BACKOFF_MILLIS = 500;

    private final AiServerProperties aiServerProperties;

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        ConnectionProvider connectionProvider = ConnectionProvider.builder("ai-server-pool")
                .maxConnections(MAX_CONNECTIONS)
                .maxIdleTime(Duration.ofSeconds(MAX_IDLE_TIME_SECONDS))
                .maxLifeTime(Duration.ofMinutes(MAX_LIFE_TIME_MINUTES))
                .pendingAcquireTimeout(Duration.ofSeconds(PENDING_ACQUIRE_TIMEOUT_SECONDS))
                .evictInBackground(Duration.ofSeconds(EVICT_IN_BACKGROUND_SECONDS))
                .build();

        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MILLIS)
                .responseTimeout(Duration.ofSeconds(aiServerProperties.getTimeoutSeconds()))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(
                                aiServerProperties.getTimeoutSeconds(), TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(
                                aiServerProperties.getTimeoutSeconds(), TimeUnit.SECONDS)));

        return builder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(loggingFilter())
                .filter(retryFilter())
                .build();
    }

    private ExchangeFilterFunction loggingFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            log.debug("[WebClient 요청] {} {}", request.method(), request.url());
            return Mono.just(request);
        });
    }

    private ExchangeFilterFunction retryFilter() {
        return (request, next) -> next.exchange(request)
                .retryWhen(Retry.backoff(MAX_RETRY_ATTEMPTS, Duration.ofMillis(RETRY_BACKOFF_MILLIS))
                        .filter(throwable -> throwable instanceof WebClientRequestException)
                        .doBeforeRetry(retrySignal ->
                                log.warn("[WebClient 재시도] 시도 #{}: {}",
                                        retrySignal.totalRetries() + 1,
                                        retrySignal.failure().getMessage())));
    }
}
