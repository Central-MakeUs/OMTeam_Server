package com.omteam.omt.chat.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.omteam.omt.chat.domain.ChatInputType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "채팅 메시지 전송 요청")
public class ChatMessageRequest {

    @Schema(
            description = "입력 타입 (TEXT: 자유 텍스트, OPTION: 선택지 선택). 빈 요청(채팅 시작)시 null",
            allowableValues = {"TEXT", "OPTION"},
            example = "TEXT"
    )
    private ChatInputType type;

    @Schema(description = "텍스트 입력 (type=TEXT일 때)", example = "운동이 너무 힘들어요")
    private String text;

    @Schema(description = "선택지 값 (type=OPTION일 때)", example = "TIME_SHORTAGE")
    private String value;

    /**
     * 채팅 시작 요청인지 확인 (빈 요청)
     */
    @JsonIgnore
    public boolean isStartRequest() {
        return type == null;
    }
}
