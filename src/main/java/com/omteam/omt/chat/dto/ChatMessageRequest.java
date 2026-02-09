package com.omteam.omt.chat.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.omteam.omt.chat.domain.ChatActionType;
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

    @Schema(description = "사용자 입력 값 (TEXT: 자유 텍스트, OPTION: 선택지 label). 채팅 내역에 표시됩니다.", example = "운동이 너무 힘들어요")
    private String value;

    @Schema(description = "선택지의 기계값 (OPTION 타입일 때 서버 로직용). TEXT 입력 시 null", example = "SUCCESS")
    private String optionValue;

    @Schema(
            description = "채팅 액션 타입. null이면 일반 AI 대화로 처리됩니다.",
            example = "COMPLETE_MISSION",
            allowableValues = {
                    "COMPLETE_MISSION",
                    "MISSION_FAILURE_REASON"
            }
    )
    private ChatActionType actionType;

    /**
     * 채팅 시작 요청인지 확인 (빈 요청)
     */
    @JsonIgnore
    public boolean isStartRequest() {
        return type == null;
    }

    /**
     * 액션 요청인지 확인
     */
    @JsonIgnore
    public boolean isActionRequest() {
        return actionType != null;
    }
}
