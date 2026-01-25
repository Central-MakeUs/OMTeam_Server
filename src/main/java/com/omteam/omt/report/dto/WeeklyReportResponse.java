package com.omteam.omt.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;

@Schema(description = "주간 AI 분석 리포트 응답")
@Builder
public record WeeklyReportResponse(
        @Schema(description = "주 시작일 (월요일)")
        LocalDate weekStartDate,

        @Schema(description = "주 종료일 (일요일)")
        LocalDate weekEndDate,

        @Schema(description = "AI 분석 결과")
        AiAnalysis aiAnalysis,

        @Schema(description = "주요 실패 원인 순위 (최대 5개)")
        List<FailureReasonRank> topFailureReasons
) {

    @Schema(description = "AI 분석 결과")
    @Builder
    public record AiAnalysis(
            @Schema(description = "주간 요약", example = "이번 주는 운동 미션에서 좋은 성과를 보였어요.")
            String summary,

            @Schema(description = "분석 인사이트", example = "평일보다 주말에 미션 성공률이 높았어요.")
            String insight,

            @Schema(description = "추천 사항", example = "평일 저녁 시간을 활용해 가벼운 운동을 시도해보세요.")
            String recommendation
    ) {
    }

    @Schema(description = "실패 원인 순위")
    @Builder
    public record FailureReasonRank(
            @Schema(description = "순위", example = "1")
            int rank,

            @Schema(description = "실패 원인", example = "시간 부족")
            String reason,

            @Schema(description = "발생 횟수", example = "3")
            int count
    ) {
    }
}
