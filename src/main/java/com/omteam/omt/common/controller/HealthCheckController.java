package com.omteam.omt.common.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "헬스 체크", description = "서버 헬스 체크 API")
@RestController
public class HealthCheckController {

    @Value("${server.env:unknown}")
    private String env;

    /**
     * 로드밸런서 헬스체크용 (인증 불필요)
     * SecurityConfig에서 /actuator/health로 허용됨
     */
    @GetMapping("/actuator/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
