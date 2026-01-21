package com.omteam.omt.onboarding.controller;

import com.omteam.omt.common.response.ApiResponse;
import com.omteam.omt.onboarding.dto.OnboardingRequest;
import com.omteam.omt.onboarding.dto.OnboardingResponse;
import com.omteam.omt.onboarding.service.OnboardingService;
import com.omteam.omt.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "온보딩", description = "온보딩 정보 등록/수정/조회 API")
@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    @Operation(
            summary = "온보딩 정보 등록",
            description = "사용자의 온보딩 정보를 등록합니다. 최초 1회만 등록 가능합니다."
    )
    @PostMapping
    public ApiResponse<OnboardingResponse> createOnboarding(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody OnboardingRequest request
    ) {
        return ApiResponse.success(
                onboardingService.createOnboarding(userPrincipal.getUserId(), request)
        );
    }

    @Operation(
            summary = "온보딩 정보 수정",
            description = "사용자의 온보딩 정보를 수정합니다."
    )
    @PutMapping
    public ApiResponse<OnboardingResponse> updateOnboarding(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody OnboardingRequest request
    ) {
        return ApiResponse.success(
                onboardingService.updateOnboarding(userPrincipal.getUserId(), request)
        );
    }

    @Operation(
            summary = "온보딩 정보 조회",
            description = "사용자의 온보딩 정보를 조회합니다."
    )
    @GetMapping
    public ApiResponse<OnboardingResponse> getOnboarding(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ApiResponse.success(
                onboardingService.getOnboarding(userPrincipal.getUserId())
        );
    }
}
