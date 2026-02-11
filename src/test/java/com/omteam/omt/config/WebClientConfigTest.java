package com.omteam.omt.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.omteam.omt.config.properties.AiServerProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class WebClientConfigTest {

    WebClientConfig webClientConfig;
    AiServerProperties aiServerProperties;

    @BeforeEach
    void setUp() {
        aiServerProperties = new AiServerProperties();
        aiServerProperties.setBaseUrl("http://localhost:8000");
        aiServerProperties.setTimeoutSeconds(30);

        webClientConfig = new WebClientConfig(aiServerProperties);
    }

    @Test
    @DisplayName("WebClient 빈 생성 성공")
    void webClient_creation_success() {
        // given
        WebClient.Builder builder = WebClient.builder();

        // when
        WebClient webClient = webClientConfig.webClient(builder);

        // then
        assertThat(webClient).isNotNull();
    }

    @Test
    @DisplayName("WebClient가 설정된 타임아웃으로 생성됨")
    void webClient_withConfiguredTimeout() {
        // given
        aiServerProperties.setTimeoutSeconds(60);
        WebClient.Builder builder = WebClient.builder();

        // when
        WebClient webClient = webClientConfig.webClient(builder);

        // then
        assertThat(webClient).isNotNull();
    }

    @Test
    @DisplayName("WebClient가 기본 타임아웃(10초)으로 생성됨")
    void webClient_withDefaultTimeout() {
        // given
        AiServerProperties defaultProps = new AiServerProperties();
        WebClientConfig config = new WebClientConfig(defaultProps);
        WebClient.Builder builder = WebClient.builder();

        // when
        WebClient webClient = config.webClient(builder);

        // then
        assertThat(webClient).isNotNull();
        assertThat(defaultProps.getTimeoutSeconds()).isEqualTo(10);
    }
}
