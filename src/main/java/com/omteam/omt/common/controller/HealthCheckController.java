package com.omteam.omt.common.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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

    /**
     * 배포 환경 확인용 (인증 필요)
     * blue-green 배포 시 현재 활성 환경 확인
     */
    @GetMapping("/env")
    public ResponseEntity<Map<String, String>> getEnv() {
        return ResponseEntity.ok(Map.of("env", env));
    }
}
