package com.omteam.omt.statistics.dto;

import com.omteam.omt.mission.domain.MissionType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

@Schema(description = "주간 통계 응답")
@Builder
public record WeeklyStatisticsResponse(
        @Schema(description = "주 시작일 (월요일)")
        LocalDate weekStartDate,

        @Schema(description = "주 종료일 (일요일)")
        LocalDate weekEndDate,

        @Schema(description = "이번주 통계")
        WeeklySummary thisWeek,

        @Schema(description = "지난주 통계")
        WeeklySummary lastWeek,

        @Schema(description = "이번주 일별 결과 목록")
        List<DailyResult> dailyResults
) {

    @Schema(description = "주간 요약 통계")
    @Builder
    public record WeeklySummary(
            @Schema(description = "수행한 총 미션 수", example = "5")
            int totalCount,

            @Schema(description = "성공 횟수", example = "4")
            int successCount,

            @Schema(description = "실패 횟수", example = "1")
            int failureCount,

            @Schema(description = "미수행 일수", example = "2")
            int notPerformedCount,

            @Schema(description = "성공률 (%)", example = "57.1")
            double successRate
    ) {
    }

    @Schema(description = "일별 미션 결과")
    @Builder
    public record DailyResult(
            @Schema(description = "날짜")
            LocalDate date,

            @Schema(description = "요일")
            DayOfWeek dayOfWeek,

            @Schema(description = "수행 상태")
            DailyStatus status,

            @Schema(description = "미션 타입", nullable = true)
            MissionType missionType,

            @Schema(description = "미션 제목", nullable = true)
            String missionTitle
    ) {
    }

    @Schema(description = "일별 미션 수행 상태")
    public enum DailyStatus {
        @Schema(description = "성공")
        SUCCESS,

        @Schema(description = "실패")
        FAILURE,

        @Schema(description = "미수행")
        NOT_PERFORMED
    }
}
