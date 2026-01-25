package com.omteam.omt.report.dto;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

@Builder
public record WeeklyReportResponse(
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        AiAnalysis aiAnalysis,
        List<FailureReasonRank> topFailureReasons
) {

    @Builder
    public record AiAnalysis(
            String summary,
            String insight,
            String recommendation
    ) {
    }

    @Builder
    public record FailureReasonRank(
            int rank,
            String reason,
            int count
    ) {
    }
}
