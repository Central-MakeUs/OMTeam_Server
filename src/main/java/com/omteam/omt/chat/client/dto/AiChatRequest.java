package com.omteam.omt.chat.client.dto;

import com.omteam.omt.chat.domain.ChatInputType;
import com.omteam.omt.chat.domain.ChatMessageRole;
import com.omteam.omt.common.ai.dto.UserContext;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiChatRequest {

    private Input input;              // null이면 채팅 시작 (인사 요청)
    private String timestamp;         // ISO 8601 형식
    private UserContext userContext;
    private List<ConversationMessage> conversationHistory;

    @Getter
    @Builder
    public static class Input {
        private ChatInputType type;   // TEXT or OPTION
        private String text;          // TEXT일 때 사용
        private String value;         // OPTION일 때 선택한 값
    }

    @Getter
    @Builder
    public static class ConversationMessage {
        private ChatMessageRole role;
        private ChatInputType type;   // USER일 때만 사용
        private String text;
        private String value;         // USER + OPTION일 때 사용
        private List<Option> options; // ASSISTANT일 때 사용
    }

    @Getter
    @Builder
    public static class Option {
        private String label;
        private String value;
    }
}
