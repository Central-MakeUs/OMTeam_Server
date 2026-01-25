package com.omteam.omt.statistics.dto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

@Builder
public record MonthlyPatternResponse(
        LocalDate startDate,
        LocalDate endDate,
        List<DayOfWeekStatistics> dayOfWeekStats,
        AiFeedback aiFeedback
) {

    @Builder
    public record DayOfWeekStatistics(
            DayOfWeek dayOfWeek,
            String dayName,
            int totalCount,
            int successCount,
            int failureCount,
            double successRate
    ) {
    }

    @Builder
    public record AiFeedback(
            String summary,
            String recommendation
    ) {
    }
}
