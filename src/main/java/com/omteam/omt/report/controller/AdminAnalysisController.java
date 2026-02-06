package com.omteam.omt.report.controller;

import com.omteam.omt.common.response.ApiResponse;
import com.omteam.omt.mission.service.MissionService;
import com.omteam.omt.report.dto.BatchProcessResult;
import com.omteam.omt.report.dto.TriggerAnalysisResponse;
import com.omteam.omt.report.service.DailyAnalysisService;
import com.omteam.omt.report.service.WeeklyAnalysisService;
import com.omteam.omt.security.principal.UserPrincipal;
import com.omteam.omt.user.domain.User;
import com.omteam.omt.user.service.UserQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "Admin - 분석", description = "분석 로직 수동 트리거 API (개발용)")
@RestController
@RequestMapping("/api/admin/analysis")
@RequiredArgsConstructor
public class AdminAnalysisController {

    private final DailyAnalysisService dailyAnalysisService;
    private final WeeklyAnalysisService weeklyAnalysisService;
    private final MissionService missionService;
    private final UserQueryService userQueryService;

    @Operation(
            summary = "일일 분석 수동 생성 (로그인 사용자)",
            description = "로그인된 사용자의 일일 분석을 수동으로 생성합니다."
    )
    @PostMapping("/daily")
    public ApiResponse<TriggerAnalysisResponse> triggerDailyAnalysis(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "대상 날짜 (기본값: 오늘)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate
    ) {
        LocalDate date = (targetDate != null) ? targetDate : LocalDate.now();
        User user = userQueryService.getUser(userPrincipal.userId());

        log.info("[Admin] 일일 분석 수동 트리거: userId={}, targetDate={}", user.getUserId(), date);
        dailyAnalysisService.generateDailyAnalysisForUser(user, date);

        return ApiResponse.success(
                TriggerAnalysisResponse.singleSuccess(
                        String.format("사용자 %d의 %s 일일 분석이 생성되었습니다.", user.getUserId(), date)
                )
        );
    }

    @Operation(
            summary = "일일 분석 수동 생성 (전체 사용자)",
            description = "모든 활성 사용자의 일일 분석을 수동으로 생성합니다."
    )
    @PostMapping("/daily/all")
    public ApiResponse<TriggerAnalysisResponse> triggerDailyAnalysisForAll(
            @Parameter(description = "대상 날짜 (기본값: 오늘)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate
    ) {
        LocalDate date = (targetDate != null) ? targetDate : LocalDate.now();

        log.info("[Admin] 전체 사용자 일일 분석 수동 트리거: targetDate={}", date);
        BatchProcessResult result = dailyAnalysisService.generateDailyEncouragementForAllUsers(date);

        return ApiResponse.success(
                TriggerAnalysisResponse.of(
                        result.totalCount(), result.successCount(),
                        String.format("%s 일일 분석이 %d명 중 %d명의 사용자에 대해 생성되었습니다.",
                                date, result.totalCount(), result.successCount())
                )
        );
    }

    @Operation(
            summary = "주간 분석 수동 생성 (로그인 사용자)",
            description = "로그인된 사용자의 주간 분석을 수동으로 생성합니다."
    )
    @PostMapping("/weekly")
    public ApiResponse<TriggerAnalysisResponse> triggerWeeklyAnalysis(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "주 시작일 (기본값: 지난주 월요일)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartDate
    ) {
        LocalDate startDate = (weekStartDate != null) ? weekStartDate : getLastWeekMonday();
        LocalDate endDate = startDate.plusDays(6);
        User user = userQueryService.getUser(userPrincipal.userId());

        log.info("[Admin] 주간 분석 수동 트리거: userId={}, weekStartDate={}", user.getUserId(), startDate);
        weeklyAnalysisService.generateWeeklyAnalysisForUser(user, startDate, endDate);

        return ApiResponse.success(
                TriggerAnalysisResponse.singleSuccess(
                        String.format("사용자 %d의 %s ~ %s 주간 분석이 생성되었습니다.",
                                user.getUserId(), startDate, endDate)
                )
        );
    }

    @Operation(
            summary = "주간 분석 수동 생성 (전체 사용자)",
            description = "모든 활성 사용자의 주간 분석을 수동으로 생성합니다."
    )
    @PostMapping("/weekly/all")
    public ApiResponse<TriggerAnalysisResponse> triggerWeeklyAnalysisForAll(
            @Parameter(description = "주 시작일 (기본값: 지난주 월요일)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartDate
    ) {
        LocalDate startDate = (weekStartDate != null) ? weekStartDate : getLastWeekMonday();

        log.info("[Admin] 전체 사용자 주간 분석 수동 트리거: weekStartDate={}", startDate);
        BatchProcessResult result = weeklyAnalysisService.generateWeeklyAnalysisForAllUsers(startDate);

        return ApiResponse.success(
                TriggerAnalysisResponse.of(
                        result.totalCount(), result.successCount(),
                        String.format("%s 주차 주간 분석이 %d명 중 %d명의 사용자에 대해 생성되었습니다.",
                                startDate, result.totalCount(), result.successCount())
                )
        );
    }

    @Operation(
            summary = "미완료 미션 만료 처리",
            description = "지정된 날짜의 미완료 미션을 만료 처리합니다."
    )
    @PostMapping("/missions/expire")
    public ApiResponse<TriggerAnalysisResponse> triggerExpireMissions(
            @Parameter(description = "대상 날짜 (기본값: 어제)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate
    ) {
        LocalDate date = (targetDate != null) ? targetDate : LocalDate.now().minusDays(1);

        log.info("[Admin] 미완료 미션 만료 처리 수동 트리거: targetDate={}", date);
        int expiredCount = missionService.expireUncompletedMissions(date);

        return ApiResponse.success(
                TriggerAnalysisResponse.of(
                        expiredCount, expiredCount,
                        String.format("%s의 미완료 미션 %d개가 만료 처리되었습니다.", date, expiredCount)
                )
        );
    }

    private LocalDate getLastWeekMonday() {
        return LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .minusWeeks(1);
    }
}
