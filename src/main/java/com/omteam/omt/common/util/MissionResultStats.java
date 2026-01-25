package com.omteam.omt.common.util;

import com.omteam.omt.mission.domain.DailyMissionResult;
import com.omteam.omt.mission.domain.MissionResult;
import java.util.List;

/**
 * 미션 결과 통계 계산을 위한 유틸리티 레코드
 */
public record MissionResultStats(
        int successCount,
        int failureCount,
        int totalCount,
        double successRate
) {

    private static final double PERCENTAGE_MULTIPLIER = 100.0;
    private static final double ROUNDING_FACTOR = 10.0;

    /**
     * DailyMissionResult 목록에서 통계를 계산
     */
    public static MissionResultStats from(List<DailyMissionResult> results) {
        int successCount = (int) results.stream()
                .filter(r -> r.getResult() == MissionResult.SUCCESS)
                .count();
        int failureCount = (int) results.stream()
                .filter(r -> r.getResult() == MissionResult.FAILURE)
                .count();
        int totalCount = successCount + failureCount;
        double successRate = calculateSuccessRate(successCount, totalCount);

        return new MissionResultStats(successCount, failureCount, totalCount, successRate);
    }

    /**
     * 기준 일수 대비 성공률을 계산
     */
    public static MissionResultStats fromWithDaysBase(List<DailyMissionResult> results, int daysElapsed) {
        int successCount = (int) results.stream()
                .filter(r -> r.getResult() == MissionResult.SUCCESS)
                .count();
        int failureCount = (int) results.stream()
                .filter(r -> r.getResult() == MissionResult.FAILURE)
                .count();
        int totalCount = successCount + failureCount;
        double successRate = daysElapsed > 0
                ? roundToOneDecimal(successCount * PERCENTAGE_MULTIPLIER / daysElapsed)
                : 0;

        return new MissionResultStats(successCount, failureCount, totalCount, successRate);
    }

    private static double calculateSuccessRate(int successCount, int totalCount) {
        if (totalCount == 0) {
            return 0;
        }
        return roundToOneDecimal(successCount * PERCENTAGE_MULTIPLIER / totalCount);
    }

    private static double roundToOneDecimal(double value) {
        return Math.round(value * ROUNDING_FACTOR) / ROUNDING_FACTOR;
    }
}
