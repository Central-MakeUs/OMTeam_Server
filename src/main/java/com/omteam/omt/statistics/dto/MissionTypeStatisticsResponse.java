package com.omteam.omt.statistics.dto;

import com.omteam.omt.mission.domain.MissionType;
import java.util.List;
import lombok.Builder;

@Builder
public record MissionTypeStatisticsResponse(
        int totalSuccessCount,
        List<TypeStatistics> byType
) {

    @Builder
    public record TypeStatistics(
            MissionType type,
            String typeName,
            int successCount,
            int failureCount,
            int totalCount,
            double successRate
    ) {
    }
}
