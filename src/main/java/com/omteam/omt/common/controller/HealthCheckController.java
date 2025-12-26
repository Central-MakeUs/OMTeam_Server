package com.omteam.omt.common.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {

    @Value("${server.env}")
    private String env;

    @GetMapping("env")
    public ResponseEntity<?> getEnv() {
        return ResponseEntity.ok(env);
    }
}
