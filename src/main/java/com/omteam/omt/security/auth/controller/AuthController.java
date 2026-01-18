package com.omteam.omt.security.auth.controller;

import com.omteam.omt.common.response.ApiResponse;
import com.omteam.omt.security.auth.dto.LoginResponse;
import com.omteam.omt.security.auth.dto.OAuthLoginRequest;
import com.omteam.omt.security.auth.service.AuthService;
import com.omteam.omt.user.domain.SocialProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/auth/oauth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 소셜 로그인
     *
     * @param provider 소셜 로그인 제공자 (google, kakao, apple)
     * @param request  idToken을 포함한 로그인 요청
     * @return 서버 발급 JWT 토큰
     */
    @PostMapping("/{provider}")
    public ApiResponse<LoginResponse> login(
            @PathVariable String provider,
            @Valid @RequestBody OAuthLoginRequest request
    ) {
        SocialProvider socialProvider = SocialProvider.from(provider);
        log.info("OAuth login request: provider={}", socialProvider);

        return ApiResponse.success(
                authService.login(socialProvider, request.getIdToken())
        );
    }
}
