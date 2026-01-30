package com.omteam.omt.report.controller;

import com.omteam.omt.common.response.ApiResponse;
import com.omteam.omt.report.dto.MonthlyPatternResponse;
import com.omteam.omt.report.dto.WeeklyReportResponse;
import com.omteam.omt.report.service.MonthlyPatternService;
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

@Tag(name = "Weekly Report", description = "주간 리포트 API")
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class WeeklyReportController {

    private final WeeklyReportService weeklyReportService;
    private final MonthlyPatternService monthlyPatternService;

    @Operation(summary = "주간 리포트 조회", description = "해당 주의 미션 성공률, 요일별 결과, AI 피드백을 조회합니다.")
    @GetMapping("/weekly")
    public ApiResponse<WeeklyReportResponse> getWeeklyReport(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "주간 시작일 (미입력시 이번 주)", example = "2024-01-15")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartDate
    ) {
        WeeklyReportResponse response = weeklyReportService.getWeeklyReport(
                principal.userId(), weekStartDate);
        return ApiResponse.success(response);
    }

    @Operation(summary = "월간 요일별 패턴 분석", description = "지난 30일간의 요일별 성공 패턴을 분석하고 AI 피드백을 제공합니다.")
    @GetMapping("/monthly-pattern")
    public ApiResponse<MonthlyPatternResponse> getMonthlyPattern(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        MonthlyPatternResponse response = monthlyPatternService.getMonthlyPattern(principal.userId());
        return ApiResponse.success(response);
    }
}
