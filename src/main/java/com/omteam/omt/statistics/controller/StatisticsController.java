package com.omteam.omt.statistics.controller;

import com.omteam.omt.common.response.ApiResponse;
import com.omteam.omt.security.principal.UserPrincipal;
import com.omteam.omt.statistics.dto.WeeklyStatisticsResponse;
import com.omteam.omt.statistics.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "통계", description = "미션 통계 관련 API")
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @Operation(
            summary = "주간 통계 조회",
            description = "이번주와 지난주의 미션 수행 통계를 비교하여 조회합니다. " +
                    "이번주 요일별 성공 여부와 미션 종류도 함께 제공합니다."
    )
    @GetMapping("/weekly")
    public ApiResponse<WeeklyStatisticsResponse> getWeeklyStatistics(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ApiResponse.success(
                statisticsService.getWeeklyStatistics(userPrincipal.getUserId())
        );
    }
}
