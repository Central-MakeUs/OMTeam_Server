package com.omteam.omt.report.dto;

import com.omteam.omt.report.domain.DailyAnalysis;
import com.omteam.omt.report.domain.EncouragementMessage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import lombok.Builder;

@Schema(description = "데일리 피드백 응답")
@Builder
public record DailyFeedbackResponse(
        @Schema(description = "피드백 대상 날짜", example = "2024-01-15")
        LocalDate targetDate,

        @Schema(description = "피드백 텍스트", example = "오늘도 열심히 운동하셨네요!")
        String feedbackText,

        @Schema(description = "격려 메시지")
        EncouragementMessageResponse encouragement
) {

    @Schema(description = "격려 메시지")
    @Builder
    public record EncouragementMessageResponse(
            @Schema(description = "제목", example = "잘하고 계세요!")
            String title,

            @Schema(description = "메시지", example = "꾸준히 노력하는 모습이 멋집니다.")
            String message
    ) {}

    /**
     * DailyAnalysis 엔티티로부터 DailyFeedbackResponse를 생성한다.
     * 4개의 encouragement 중 null이 아닌 첫 번째 메시지를 사용한다.
     */
    public static DailyFeedbackResponse from(DailyAnalysis analysis) {
        EncouragementMessage selectedMessage = selectFirstNonNullEncouragement(analysis);

        EncouragementMessageResponse encouragementResponse = null;
        if (selectedMessage != null) {
            encouragementResponse = EncouragementMessageResponse.builder()
                    .title(selectedMessage.getTitle())
                    .message(selectedMessage.getMessage())
                    .build();
        }

        return DailyFeedbackResponse.builder()
                .targetDate(analysis.getTargetDate())
                .feedbackText(analysis.getFeedbackText())
                .encouragement(encouragementResponse)
                .build();
    }

    /**
     * 4개의 encouragement 중 null이 아닌 첫 번째 메시지를 반환한다.
     * 우선순위: praise > retry > normal > push
     */
    private static EncouragementMessage selectFirstNonNullEncouragement(DailyAnalysis analysis) {
        if (analysis.getPraise() != null) {
            return analysis.getPraise();
        }
        if (analysis.getRetry() != null) {
            return analysis.getRetry();
        }
        if (analysis.getNormal() != null) {
            return analysis.getNormal();
        }
        if (analysis.getPush() != null) {
            return analysis.getPush();
        }
        return null;
    }
}
