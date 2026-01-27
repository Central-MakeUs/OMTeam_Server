package com.omteam.omt.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omteam.omt.config.properties.AiServerProperties;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * AI 서버 API 통합 테스트를 위한 베이스 클래스.
 * MockWebServer를 사용하여 AI 서버 응답을 모킹합니다.
 */
@Tag("integration")
@SpringBootTest
@ActiveProfiles("integration-test")
public abstract class IntegrationTestBase {

    protected static MockWebServer mockWebServer;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected AiServerProperties aiServerProperties;

    @MockitoBean
    protected RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    void setUpMockServer() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start(18080);
    }

    @AfterEach
    void tearDownMockServer() throws IOException {
        if (mockWebServer != null) {
            mockWebServer.shutdown();
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("ai-server.base-url", () -> "http://localhost:18080");
    }
}
