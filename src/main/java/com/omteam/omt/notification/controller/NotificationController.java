package com.omteam.omt.notification.controller;

import com.omteam.omt.common.response.ApiResponse;
import com.omteam.omt.notification.dto.FcmTokenRequest;
import com.omteam.omt.notification.service.NotificationService;
import com.omteam.omt.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "알림", description = "FCM 토큰 관리 API")
@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(
            summary = "FCM 토큰 등록/갱신",
            description = "디바이스의 FCM 토큰을 서버에 등록하거나 갱신합니다."
    )
    @PutMapping("/fcm-token")
    public ApiResponse<Void> registerFcmToken(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody FcmTokenRequest request
    ) {
        notificationService.registerFcmToken(userPrincipal.userId(), request.getFcmToken());
        return ApiResponse.success(null);
    }

    @Operation(
            summary = "FCM 토큰 삭제",
            description = "서버에 등록된 FCM 토큰을 삭제합니다. 로그아웃 또는 알림 비활성화 시 사용합니다."
    )
    @DeleteMapping("/fcm-token")
    public ApiResponse<Void> deleteFcmToken(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        notificationService.deleteFcmToken(userPrincipal.userId());
        return ApiResponse.success(null);
    }
}
