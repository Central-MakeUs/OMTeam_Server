package com.omteam.omt.character.dto;

import lombok.Builder;

@Builder
public record CharacterResponse(
        int level,
        int experiencePercent,
        int successCount,
        int successCountUntilNextLevel,
        String encouragementTitle,
        String encouragementMessage
) {
}
