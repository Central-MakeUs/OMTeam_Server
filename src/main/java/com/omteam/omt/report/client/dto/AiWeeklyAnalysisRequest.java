package com.omteam.omt.report.client.dto;

import com.omteam.omt.common.ai.dto.UserContext;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiWeeklyAnalysisRequest {

    private Long userId;
    private UserContext userContext;
    private LocalDate weekStartDate;
    private LocalDate weekEndDate;
    private List<String> failureReasons;
    private List<DailyResultSummary> weeklyResults;
    private List<DayOfWeekStats> monthlyDayOfWeekStats;

    @Getter
    @Builder
    public static class DailyResultSummary {
        private LocalDate date;
        private String dayOfWeek;
        private String status;  // SUCCESS, FAILURE, NOT_PERFORMED
        private String missionType;
        private String failureReason;
    }

    @Getter
    @Builder
    public static class DayOfWeekStats {
        private String dayOfWeek;
        private int totalCount;
        private int successCount;
        private double successRate;
    }

    public static AiWeeklyAnalysisRequest of(
            Long userId,
            UserContext userContext,
            LocalDate weekStartDate,
            LocalDate weekEndDate,
            List<String> failureReasons,
            List<DailyResultSummary> weeklyResults,
            List<DayOfWeekStats> monthlyDayOfWeekStats
    ) {
        return AiWeeklyAnalysisRequest.builder()
                .userId(userId)
                .userContext(userContext)
                .weekStartDate(weekStartDate)
                .weekEndDate(weekEndDate)
                .failureReasons(failureReasons != null ? failureReasons : List.of())
                .weeklyResults(weeklyResults != null ? weeklyResults : List.of())
                .monthlyDayOfWeekStats(monthlyDayOfWeekStats != null ? monthlyDayOfWeekStats : List.of())
                .build();
    }
}
