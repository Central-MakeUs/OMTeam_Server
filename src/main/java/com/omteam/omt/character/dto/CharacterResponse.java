package com.omteam.omt.character.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "캐릭터 정보 응답")
@Builder
public record CharacterResponse(
        @Schema(description = "현재 레벨", example = "2")
        int level,

        @Schema(description = "현재 레벨의 경험치 퍼센트 (0-100)", example = "45")
        int experiencePercent,

        @Schema(description = "총 미션 성공 횟수", example = "35")
        int successCount,

        @Schema(description = "다음 레벨까지 남은 성공 횟수", example = "25")
        int successCountUntilNextLevel,

        @Schema(description = "오늘의 격려 메시지 제목", example = "오늘도 힘내세요!")
        String encouragementTitle,

        @Schema(description = "오늘의 격려 메시지 내용", example = "어제 멋지게 해냈잖아요. 오늘도 할 수 있어요!")
        String encouragementMessage
) {
}
