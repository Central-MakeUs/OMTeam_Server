package com.omteam.omt.security.auth.controller;

import com.omteam.omt.common.response.ApiResponse;
import com.omteam.omt.security.auth.dto.LoginResponse;
import com.omteam.omt.security.auth.dto.OAuthLoginRequest;
import com.omteam.omt.security.auth.dto.RefreshTokenRequest;
import com.omteam.omt.security.auth.service.AuthService;
import com.omteam.omt.security.principal.UserPrincipal;
import com.omteam.omt.user.domain.SocialProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증", description = "소셜 로그인 및 토큰 관리 API")
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "소셜 로그인",
            description = "Google, Kakao, Apple의 idToken을 검증하여 서버 자체 JWT 토큰을 발급합니다."
    )
    @PostMapping("/oauth/{provider}")
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

    @Operation(
            summary = "로그아웃",
            description = "Refresh Token을 무효화합니다. Access Token은 만료될 때까지 유효합니다."
    )
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal UserPrincipal principal) {
        log.info("Logout request: userId={}", principal.userId());
        authService.logout(principal.userId());
        return ApiResponse.success(null);
    }

    @Operation(
            summary = "토큰 갱신",
            description = "Refresh Token을 사용하여 새로운 Access Token과 Refresh Token을 발급받습니다."
    )
    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Token refresh request");
        return ApiResponse.success(
                authService.refreshToken(request.getRefreshToken())
        );
    }

    @Operation(
            summary = "회원 탈퇴",
            description = "회원 탈퇴를 처리합니다. Soft Delete 방식으로 동작하며, 탈퇴 후 동일 소셜 계정으로 재가입 가능합니다."
    )
    @PostMapping("/withdraw")
    public ApiResponse<Void> withdraw(@AuthenticationPrincipal UserPrincipal principal) {
        log.info("Withdraw request: userId={}", principal.userId());
        authService.withdraw(principal.userId());
        return ApiResponse.success(null);
    }
}
