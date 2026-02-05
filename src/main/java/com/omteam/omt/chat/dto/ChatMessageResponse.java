package com.omteam.omt.chat.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omteam.omt.chat.domain.ChatMessage;
import com.omteam.omt.chat.domain.ChatMessageRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "채팅 메시지 응답")
public class ChatMessageResponse {

    @Schema(description = "메시지 ID", example = "123")
    private Long messageId;

    @Schema(description = "발신자 역할", allowableValues = {"USER", "ASSISTANT"}, example = "ASSISTANT")
    private ChatMessageRole role;

    @Schema(description = "메시지 내용", example = "안녕하세요! 무엇을 도와드릴까요?")
    private String content;

    @Schema(description = "선택지 목록 (ASSISTANT 메시지에만 존재)")
    private List<Option> options;

    @Schema(description = "대화 종료 메시지 여부", example = "false")
    private boolean isTerminal;

    @Schema(description = "메시지 생성 시간", example = "2026-01-15T10:30:00")
    private LocalDateTime createdAt;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Option {
        @Schema(description = "선택지 라벨", example = "운동이 힘들어요")
        private String label;

        @Schema(description = "선택지 값", example = "EXERCISE_HARD")
        private String value;
    }

    public static ChatMessageResponse from(ChatMessage message, ObjectMapper objectMapper, boolean isTerminal) {
        return ChatMessageResponse.builder()
                .messageId(message.getId())
                .role(message.getRole())
                .content(message.getContent())
                .options(parseOptions(message.getOptions(), objectMapper))
                .isTerminal(message.isTerminal() || isTerminal)
                .createdAt(message.getCreatedAt())
                .build();
    }

    public static List<ChatMessageResponse> fromList(List<ChatMessage> messages, ObjectMapper objectMapper) {
        return messages.stream()
                .map(msg -> ChatMessageResponse.from(msg, objectMapper, false))
                .toList();
    }

    private static List<Option> parseOptions(String optionsJson, ObjectMapper objectMapper) {
        if (optionsJson == null || optionsJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(optionsJson, new TypeReference<List<Option>>() {});
        } catch (Exception e) {
            log.warn("옵션 JSON 파싱 실패: {}", optionsJson, e);
            return List.of();
        }
    }
}
