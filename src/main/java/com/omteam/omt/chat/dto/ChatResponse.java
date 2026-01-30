package com.omteam.omt.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "채팅 내역 조회 응답")
public class ChatResponse {

    @Schema(description = "활성 세션 존재 여부", example = "true")
    private boolean hasActiveSession;

    @Schema(description = "더 많은 메시지 존재 여부 (페이지네이션)", example = "true")
    private boolean hasMore;

    @Schema(description = "다음 페이지 커서 (마지막 메시지 ID)", example = "15")
    private Long nextCursor;

    @Schema(description = "메시지 목록 (최신순)")
    private List<ChatMessageResponse> messages;
}
