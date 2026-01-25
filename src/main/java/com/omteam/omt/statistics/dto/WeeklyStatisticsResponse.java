package com.omteam.omt.statistics.dto;

import com.omteam.omt.mission.domain.MissionType;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

@Builder
public record WeeklyStatisticsResponse(
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        WeeklySummary thisWeek,
        WeeklySummary lastWeek,
        List<DailyResult> dailyResults
) {

    @Builder
    public record WeeklySummary(
            int totalCount,
            int successCount,
            int failureCount,
            int notPerformedCount,
            double successRate
    ) {
    }

    @Builder
    public record DailyResult(
            LocalDate date,
            DayOfWeek dayOfWeek,
            DailyStatus status,
            MissionType missionType,
            String missionTitle
    ) {
    }

    public enum DailyStatus {
        SUCCESS,
        FAILURE,
        NOT_PERFORMED
    }
}
