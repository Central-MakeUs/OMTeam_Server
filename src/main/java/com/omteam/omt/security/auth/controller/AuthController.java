package com.omteam.omt.security.auth.controller;

import com.omteam.omt.common.response.ApiResponse;

import com.omteam.omt.security.auth.dto.AppleAuthCodeRequest;
import com.omteam.omt.security.auth.dto.GoogleAuthCodeRequest;
import com.omteam.omt.security.auth.dto.KakaoAuthCodeRequest;
import com.omteam.omt.security.auth.dto.LoginResponse;
import com.omteam.omt.security.auth.service.AuthService;
import com.omteam.omt.user.domain.SocialProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth/oauth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/google")
    public ApiResponse<LoginResponse> googleLogin(
            @RequestBody GoogleAuthCodeRequest request
    ) {
        log.info("google request code: {}", request.getAuthorizationCode());
        return ApiResponse.success(
                authService.login(SocialProvider.GOOGLE, request.getAuthorizationCode())
        );
    }

    @PostMapping("/kakao")
    public ApiResponse<LoginResponse> kakaoLogin(
            @RequestBody KakaoAuthCodeRequest request
    ) {
        log.info("kakao request code: {}", request.getAuthorizationCode());
        return ApiResponse.success(
                authService.login(SocialProvider.KAKAO, request.getAuthorizationCode())
        );
    }

    @PostMapping("/apple")
    public ApiResponse<LoginResponse> appleLogin(
            @RequestBody AppleAuthCodeRequest request
    ) {
        log.info("apple login request");
        return ApiResponse.success(authService.login(SocialProvider.APPLE, request.getIdToken())
        );
    }

}
