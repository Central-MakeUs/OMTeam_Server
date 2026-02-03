package com.omteam.omt.report.dto;

import com.omteam.omt.mission.domain.MissionType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

@Schema(description = "주간 리포트 응답")
@Builder
public record WeeklyReportResponse(
        @Schema(description = "주간 시작일 (월요일)", example = "2024-01-15")
        LocalDate weekStartDate,

        @Schema(description = "주간 종료일 (일요일)", example = "2024-01-21")
        LocalDate weekEndDate,

        @Schema(description = "이번 주 성공률 (%)", example = "71.4")
        double thisWeekSuccessRate,

        @Schema(description = "지난 주 성공률 (%)", example = "57.1")
        double lastWeekSuccessRate,

        @Schema(description = "이번 주 성공 횟수", example = "3")
        long thisWeekSuccessCount,

        @Schema(description = "요일별 미션 결과 목록")
        List<DailyResult> dailyResults,

        @Schema(description = "미션 타입별 성공 횟수")
        List<TypeSuccessCount> typeSuccessCounts,

        @Schema(description = "실패 원인 순위 (상위 3개)")
        List<FailureReasonRank> topFailureReasons,

        @Schema(description = "AI 피드백")
        AiFeedback aiFeedback
) {

    @Schema(description = "요일별 미션 결과")
    @Builder
    public record DailyResult(
            @Schema(description = "날짜", example = "2024-01-15")
            LocalDate date,

            @Schema(description = "요일", example = "MONDAY")
            DayOfWeek dayOfWeek,

            @Schema(description = "미션 수행 상태", allowableValues = {"SUCCESS", "FAILURE", "NOT_PERFORMED"})
            DailyStatus status,

            @Schema(description = "미션 타입", allowableValues = {"EXERCISE", "DIET"})
            MissionType missionType,

            @Schema(description = "미션 제목", example = "30분 걷기")
            String missionTitle
    ) {}

    @Schema(description = "미션 수행 상태")
    public enum DailyStatus {
        @Schema(description = "성공")
        SUCCESS,

        @Schema(description = "실패")
        FAILURE,

        @Schema(description = "미수행")
        NOT_PERFORMED
    }

    @Schema(description = "미션 타입별 성공 횟수")
    @Builder
    public record TypeSuccessCount(
            @Schema(description = "미션 타입", allowableValues = {"EXERCISE", "DIET"})
            MissionType type,

            @Schema(description = "미션 타입 한글명", example = "운동")
            String typeName,

            @Schema(description = "성공 횟수", example = "3")
            int successCount
    ) {}

    @Schema(description = "실패 원인 순위")
    @Builder
    public record FailureReasonRank(
            @Schema(description = "순위", example = "1")
            int rank,

            @Schema(description = "실패 원인", example = "시간 부족")
            String reason,

            @Schema(description = "발생 횟수", example = "5")
            int count
    ) {}

    @Schema(description = "AI 피드백")
    @Builder
    public record AiFeedback(
            @Schema(description = "이번주 실패 원인 순위 (AI 카테고리화)")
            List<AiFailureReasonRank> failureReasonRanking,

            @Schema(description = "이번주 결과에 대한 피드백")
            String weeklyFeedback
    ) {}

    @Schema(description = "AI 분석 실패 원인 순위")
    @Builder
    public record AiFailureReasonRank(
            @Schema(description = "순위", example = "1")
            int rank,

            @Schema(description = "카테고리 (AI가 분류)", example = "시간 부족")
            String category,

            @Schema(description = "발생 횟수", example = "3")
            int count
    ) {}
}
