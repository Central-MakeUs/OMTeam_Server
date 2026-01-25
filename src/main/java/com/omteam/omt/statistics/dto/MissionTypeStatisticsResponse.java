package com.omteam.omt.statistics.dto;

import com.omteam.omt.mission.domain.MissionType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

@Schema(description = "미션 종류별 통계 응답")
@Builder
public record MissionTypeStatisticsResponse(
        @Schema(description = "전체 미션 성공 횟수", example = "42")
        int totalSuccessCount,

        @Schema(description = "종류별 통계 목록")
        List<TypeStatistics> byType
) {

    @Schema(description = "미션 종류별 통계")
    @Builder
    public record TypeStatistics(
            @Schema(description = "미션 종류")
            MissionType type,

            @Schema(description = "미션 종류 한글명", example = "운동")
            String typeName,

            @Schema(description = "성공 횟수", example = "25")
            int successCount,

            @Schema(description = "실패 횟수", example = "5")
            int failureCount,

            @Schema(description = "총 수행 횟수", example = "30")
            int totalCount,

            @Schema(description = "성공률 (%)", example = "83.3")
            double successRate
    ) {
    }
}
