package com.omteam.omt.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

@Schema(description = "월간 요일별 패턴 분석 응답")
@Builder
public record MonthlyPatternResponse(
        @Schema(description = "분석 시작일")
        LocalDate startDate,

        @Schema(description = "분석 종료일")
        LocalDate endDate,

        @Schema(description = "요일별 통계 목록")
        List<DayOfWeekStatistics> dayOfWeekStats,

        @Schema(description = "AI 피드백 (월간 요일)")
        AiFeedback aiFeedback
) {

    @Schema(description = "요일별 통계")
    @Builder
    public record DayOfWeekStatistics(
            @Schema(description = "요일")
            DayOfWeek dayOfWeek,

            @Schema(description = "요일 한글명", example = "월요일")
            String dayName,

            @Schema(description = "총 수행 횟수", example = "4")
            int totalCount,

            @Schema(description = "성공 횟수", example = "3")
            int successCount,

            @Schema(description = "실패 횟수", example = "1")
            int failureCount,

            @Schema(description = "성공률 (%)", example = "75.0")
            double successRate
    ) {
    }

    @Schema(name = "MonthlyAiFeedback", description = "AI 피드백 (월간 요일)")
    @Builder
    public record AiFeedback(
            @Schema(description = "요일별 피드백 제목", example = "화요일에 집중해보세요")
            String dayOfWeekFeedbackTitle,

            @Schema(description = "요일별 피드백 내용")
            String dayOfWeekFeedbackContent
    ) {
    }
}
