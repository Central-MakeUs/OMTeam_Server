package com.omteam.omt.report.dto;

import com.omteam.omt.report.domain.DailyAnalysis;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Builder;

@Schema(description = "데일리 피드백 응답")
@Builder
public record DailyFeedbackResponse(
        @Schema(description = "데이터 상태")
        ReportDataStatus dataStatus,

        @Schema(description = "피드백 대상 날짜", example = "2024-01-15")
        LocalDate targetDate,

        @Schema(description = "피드백 텍스트", example = "오늘도 열심히 운동하셨네요!")
        String feedbackText,

        @Schema(description = "기본 메시지 여부 (true이면 AI 분석이 아닌 안내 메시지)")
        boolean isDefault
) {
    public static DailyFeedbackResponse from(DailyAnalysis analysis) {

        return new DailyFeedbackResponse(
                ReportDataStatus.READY,
                analysis.getTargetDate(),
                analysis.getFeedbackText(),
                false);
    }
}
