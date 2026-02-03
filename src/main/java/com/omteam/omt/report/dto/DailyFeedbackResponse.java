package com.omteam.omt.report.dto;

import com.omteam.omt.report.domain.DailyAnalysis;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Builder;

@Schema(description = "데일리 피드백 응답")
@Builder
public record DailyFeedbackResponse(
        @Schema(description = "피드백 대상 날짜", example = "2024-01-15")
        LocalDate targetDate,

        @Schema(description = "피드백 텍스트", example = "오늘도 열심히 운동하셨네요!")
        String feedbackText
) {
    public static DailyFeedbackResponse from(DailyAnalysis analysis) {

        return new DailyFeedbackResponse(
                analysis.getTargetDate(),
                analysis.getFeedbackText());
    }
}
