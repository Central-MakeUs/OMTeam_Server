package com.omteam.omt.mission.controller;

import com.omteam.omt.common.response.ApiResponse;
import com.omteam.omt.mission.dto.DailyMissionRecommendResponse;
import com.omteam.omt.mission.dto.MissionResultRequest;
import com.omteam.omt.mission.dto.MissionResultResponse;
import com.omteam.omt.mission.dto.MissionSelectRequest;
import com.omteam.omt.mission.dto.RecommendedMissionResponse;
import com.omteam.omt.mission.dto.TodayMissionStatusResponse;
import com.omteam.omt.mission.service.MissionService;
import com.omteam.omt.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "미션", description = "데일리 미션 관련 API")
@RestController
@RequestMapping("/api/missions")
@RequiredArgsConstructor
public class MissionController {

    private final MissionService missionService;

    @Operation(
            summary = "데일리 미션 추천 받기",
            description = "AI 서버를 통해 오늘의 추천 미션 목록을 받습니다. " +
                    "기존 추천 미션이 있으면 만료 처리하고 새로 추천받습니다."
    )
    @PostMapping("/daily/recommend")
    public ApiResponse<DailyMissionRecommendResponse> recommendDailyMissions(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ApiResponse.success(
                missionService.recommendDailyMissions(userPrincipal.userId())
        );
    }

    @Operation(
            summary = "오늘의 추천 미션 목록 조회",
            description = "오늘 추천받은 미션 목록을 조회합니다."
    )
    @GetMapping("/daily/recommendations")
    public ApiResponse<List<RecommendedMissionResponse>> getTodayRecommendations(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ApiResponse.success(
                missionService.getTodayRecommendations(userPrincipal.userId())
        );
    }

    @Operation(
            summary = "미션 시작",
            description = "추천된 미션 중 하나를 선택하여 시작합니다. " +
                    "진행 중인 미션이 있으면 자동으로 포기 처리 후 새 미션을 시작합니다."
    )
    @PostMapping("/daily/start")
    public ApiResponse<RecommendedMissionResponse> startMission(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody MissionSelectRequest request
    ) {
        return ApiResponse.success(
                missionService.startMission(userPrincipal.userId(), request.getRecommendedMissionId())
        );
    }

    @Operation(
            summary = "미션 결과 등록",
            description = "진행 중인 미션의 성공/실패 결과를 등록합니다."
    )
    @PostMapping("/daily/complete")
    public ApiResponse<MissionResultResponse> completeMission(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody MissionResultRequest request
    ) {
        return ApiResponse.success(
                missionService.completeMission(userPrincipal.userId(), request)
        );
    }

    @Operation(
            summary = "오늘의 미션 상태 조회",
            description = "오늘의 미션 진행 상태를 조회합니다. " +
                    "(추천 여부, 선택 여부, 진행 중 여부, 완료 여부)"
    )
    @GetMapping("/daily/status")
    public ApiResponse<TodayMissionStatusResponse> getTodayMissionStatus(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ApiResponse.success(
                missionService.getTodayMissionStatus(userPrincipal.userId())
        );
    }
}
