package com.omteam.omt.chat.controller;

import com.omteam.omt.chat.dto.ChatMessageRequest;
import com.omteam.omt.chat.dto.ChatMessageResponse;
import com.omteam.omt.chat.dto.ChatResponse;
import com.omteam.omt.chat.service.ChatService;
import com.omteam.omt.common.response.ApiResponse;
import com.omteam.omt.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "챗봇", description = "AI 챗봇 대화 API")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Operation(
            summary = "대화 내역 조회",
            description = "사용자의 채팅 대화 내역을 조회합니다. 커서 기반 페이지네이션을 지원합니다."
    )
    @GetMapping
    public ApiResponse<ChatResponse> getChatHistory(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "마지막으로 조회한 메시지 ID (없으면 최신부터)")
            @RequestParam(required = false) Long cursor,
            @Parameter(description = "조회할 메시지 수 (기본값: 20)")
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(
                chatService.getChatHistory(userPrincipal.userId(), cursor, size)
        );
    }

    @Operation(
            summary = "메시지 전송",
            description = "챗봇에게 메시지를 전송하고 응답을 받습니다. " +
                    "빈 요청은 채팅 시작(AI 인사)으로 처리됩니다. " +
                    "세션이 없으면 자동 생성됩니다."
    )
    @PostMapping("/messages")
    public ApiResponse<ChatMessageResponse> sendMessage(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody(required = false) ChatMessageRequest request
    ) {
        return ApiResponse.success(
                chatService.sendMessage(userPrincipal.userId(), request)
        );
    }
}
