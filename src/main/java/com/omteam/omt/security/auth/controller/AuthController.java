package com.omteam.omt.security.auth.controller;

import com.omteam.omt.common.response.ApiResponse;
import com.omteam.omt.security.auth.dto.LoginResponse;
import com.omteam.omt.security.auth.dto.OAuthLoginRequest;
import com.omteam.omt.security.auth.service.AuthService;
import com.omteam.omt.user.domain.SocialProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증", description = "소셜 로그인 API")
@Slf4j
@RestController
@RequestMapping("/auth/oauth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "소셜 로그인",
            description = "Google, Kakao, Apple의 idToken을 검증하여 서버 자체 JWT 토큰을 발급합니다."
    )
    @PostMapping("/{provider}")
    public ApiResponse<LoginResponse> login(
            @Parameter(
                    description = "소셜 로그인 제공자",
                    example = "google",
                    schema = @Schema(allowableValues = {"google", "kakao", "apple"})
            )
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
