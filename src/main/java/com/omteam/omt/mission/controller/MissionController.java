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
                missionService.recommendDailyMissions(userPrincipal.getUserId())
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
                missionService.getTodayRecommendations(userPrincipal.getUserId())
        );
    }

    @Operation(
            summary = "미션 선택",
            description = "추천된 미션 중 하나를 선택합니다."
    )
    @PostMapping("/daily/select")
    public ApiResponse<RecommendedMissionResponse> selectMission(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody MissionSelectRequest request
    ) {
        return ApiResponse.success(
                missionService.selectMission(userPrincipal.getUserId(), request.getRecommendedMissionId())
        );
    }

    @Operation(
            summary = "미션 시작",
            description = "선택한 미션을 시작합니다. 미션을 먼저 선택해야 합니다."
    )
    @PostMapping("/daily/start")
    public ApiResponse<RecommendedMissionResponse> startMission(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ApiResponse.success(
                missionService.startMission(userPrincipal.getUserId())
        );
    }

    @Operation(
            summary = "미션 다시 선택",
            description = "진행 중이거나 선택된 미션을 취소하고 다시 선택할 수 있게 합니다."
    )
    @PostMapping("/daily/reselect")
    public ApiResponse<RecommendedMissionResponse> reselectMission(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        return ApiResponse.success(
                missionService.reselectMission(userPrincipal.getUserId())
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
                missionService.completeMission(userPrincipal.getUserId(), request)
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
                missionService.getTodayMissionStatus(userPrincipal.getUserId())
        );
    }
}
