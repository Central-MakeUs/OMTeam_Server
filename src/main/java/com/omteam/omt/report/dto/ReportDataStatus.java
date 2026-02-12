package com.omteam.omt.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "리포트 데이터 상태")
public enum ReportDataStatus {

    @Schema(description = "AI 분석 결과 존재")
    READY,

    @Schema(description = "미션 데이터는 있으나 AI 분석 미생성")
    PENDING_ANALYSIS,

    @Schema(description = "미션 데이터 없음")
    NO_DATA
}
