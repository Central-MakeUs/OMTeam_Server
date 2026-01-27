package com.omteam.omt.report.client.dto;

import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiWeeklyAnalysisRequest {

    private Long userId;
    private LocalDate weekStartDate;
    private LocalDate weekEndDate;
    private List<String> failureReasons;

    public static AiWeeklyAnalysisRequest of(
            Long userId,
            LocalDate weekStartDate,
            LocalDate weekEndDate,
            List<String> failureReasons
    ) {
        return AiWeeklyAnalysisRequest.builder()
                .userId(userId)
                .weekStartDate(weekStartDate)
                .weekEndDate(weekEndDate)
                .failureReasons(failureReasons != null ? failureReasons : List.of())
                .build();
    }
}
