package com.omteam.omt.report.controller;

import com.omteam.omt.common.response.ApiResponse;
import com.omteam.omt.report.dto.DailyFeedbackResponse;
import com.omteam.omt.report.dto.MonthlyPatternResponse;
import com.omteam.omt.report.dto.WeeklyReportResponse;
import com.omteam.omt.report.service.DailyAnalysisService;
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

@Tag(name = "주간 리포트", description = "주간 분석 및 통계 조회 API")
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class WeeklyReportController {

    private final WeeklyReportService weeklyReportService;
    private final MonthlyPatternService monthlyPatternService;
    private final DailyAnalysisService dailyAnalysisService;

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

    @Operation(summary = "데일리 피드백 조회", description = "특정 날짜의 AI 피드백 메시지를 조회합니다.")
    @GetMapping("/daily/feedback")
    public ApiResponse<DailyFeedbackResponse> getDailyFeedback(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "조회할 날짜 (미입력시 오늘)", example = "2024-01-15")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        DailyFeedbackResponse response = dailyAnalysisService.getDailyFeedback(
                principal.userId(), date);
        return ApiResponse.success(response);
    }
}
