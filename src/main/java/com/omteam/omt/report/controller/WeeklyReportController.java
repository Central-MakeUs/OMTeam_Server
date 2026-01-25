package com.omteam.omt.report.controller;

import com.omteam.omt.common.response.ApiResponse;
import com.omteam.omt.report.dto.WeeklyReportResponse;
import com.omteam.omt.report.service.WeeklyReportService;
import com.omteam.omt.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "리포트", description = "주간/월간 리포트 관련 API")
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class WeeklyReportController {

    private final WeeklyReportService weeklyReportService;

    @Operation(
            summary = "주간 AI 분석 리포트 조회",
            description = "특정 주의 AI 분석 결과와 주요 실패 원인 순위를 조회합니다. " +
                    "weekStartDate를 생략하면 이번 주 리포트를 조회합니다."
    )
    @GetMapping("/weekly")
    public ApiResponse<WeeklyReportResponse> getWeeklyReport(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "주 시작일 (월요일, YYYY-MM-DD 형식)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartDate
    ) {
        return ApiResponse.success(
                weeklyReportService.getWeeklyReport(userPrincipal.getUserId(), weekStartDate)
        );
    }
}
