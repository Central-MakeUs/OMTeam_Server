package com.omteam.omt.mission.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import com.omteam.omt.common.response.ApiResponse;
import com.omteam.omt.mission.domain.MissionResult;
import com.omteam.omt.mission.domain.MissionType;
import com.omteam.omt.mission.domain.RecommendedMissionStatus;
import com.omteam.omt.mission.dto.DailyMissionRecommendResponse;
import com.omteam.omt.mission.dto.MissionResponse;
import com.omteam.omt.mission.dto.MissionResultRequest;
import com.omteam.omt.mission.dto.MissionResultResponse;
import com.omteam.omt.mission.dto.MissionSelectRequest;
import com.omteam.omt.mission.dto.RecommendedMissionResponse;
import com.omteam.omt.mission.dto.TodayMissionStatusResponse;
import com.omteam.omt.mission.service.MissionService;
import com.omteam.omt.security.principal.UserPrincipal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("[단위] MissionController")
class MissionControllerTest {

    @Mock
    MissionService missionService;

    @InjectMocks
    MissionController missionController;

    UserPrincipal principal;

    @BeforeEach
    void setUp() {
        principal = new UserPrincipal(1L);
    }

    @Nested
    @DisplayName("데일리 미션 추천 받기")
    class RecommendDailyMissions {

        @Test
        @DisplayName("성공 - 3개의 추천 미션이 반환된다")
        void success() {
            // given
            DailyMissionRecommendResponse response = DailyMissionRecommendResponse.builder()
                    .missionDate(LocalDate.now())
                    .recommendations(List.of(
                            createRecommendedMission(1L, "스트레칭 10분", RecommendedMissionStatus.RECOMMENDED),
                            createRecommendedMission(2L, "계단 오르기", RecommendedMissionStatus.RECOMMENDED),
                            createRecommendedMission(3L, "샐러드 먹기", RecommendedMissionStatus.RECOMMENDED)
                    ))
                    .hasInProgressMission(false)
                    .build();

            given(missionService.recommendDailyMissions(principal.userId()))
                    .willReturn(response);

            // when
            ApiResponse<DailyMissionRecommendResponse> result =
                    missionController.recommendDailyMissions(principal);

            // then
            assertThat(result.success()).isTrue();
            assertThat(result.data()).isNotNull();
            assertThat(result.data().getRecommendations()).hasSize(3);
            assertThat(result.data().getMissionDate()).isEqualTo(LocalDate.now());
            assertThat(result.data().isHasInProgressMission()).isFalse();

            then(missionService).should().recommendDailyMissions(principal.userId());
        }

        @Test
        @DisplayName("성공 - 진행 중인 미션이 있는 경우")
        void successWithInProgressMission() {
            // given
            RecommendedMissionResponse inProgressMission =
                    createRecommendedMission(1L, "요가 20분", RecommendedMissionStatus.IN_PROGRESS);

            DailyMissionRecommendResponse response = DailyMissionRecommendResponse.builder()
                    .missionDate(LocalDate.now())
                    .recommendations(List.of(inProgressMission))
                    .hasInProgressMission(true)
                    .inProgressMission(inProgressMission)
                    .build();

            given(missionService.recommendDailyMissions(principal.userId()))
                    .willReturn(response);

            // when
            ApiResponse<DailyMissionRecommendResponse> result =
                    missionController.recommendDailyMissions(principal);

            // then
            assertThat(result.success()).isTrue();
            assertThat(result.data().isHasInProgressMission()).isTrue();
            assertThat(result.data().getInProgressMission()).isNotNull();
        }
    }

    @Nested
    @DisplayName("오늘의 추천 미션 목록 조회")
    class GetTodayRecommendations {

        @Test
        @DisplayName("성공 - 추천 미션 목록이 반환된다")
        void success() {
            // given
            List<RecommendedMissionResponse> recommendations = List.of(
                    createRecommendedMission(1L, "스트레칭 10분", RecommendedMissionStatus.RECOMMENDED),
                    createRecommendedMission(2L, "걷기 30분", RecommendedMissionStatus.RECOMMENDED)
            );

            given(missionService.getTodayRecommendations(principal.userId()))
                    .willReturn(recommendations);

            // when
            ApiResponse<List<RecommendedMissionResponse>> result =
                    missionController.getTodayRecommendations(principal);

            // then
            assertThat(result.success()).isTrue();
            assertThat(result.data()).hasSize(2);

            then(missionService).should().getTodayRecommendations(principal.userId());
        }

        @Test
        @DisplayName("성공 - 추천 미션이 없는 경우 빈 리스트 반환")
        void successEmptyList() {
            // given
            given(missionService.getTodayRecommendations(principal.userId()))
                    .willReturn(List.of());

            // when
            ApiResponse<List<RecommendedMissionResponse>> result =
                    missionController.getTodayRecommendations(principal);

            // then
            assertThat(result.success()).isTrue();
            assertThat(result.data()).isEmpty();
        }
    }

    @Nested
    @DisplayName("미션 시작")
    class StartMission {

        @Test
        @DisplayName("성공 - 선택한 미션이 IN_PROGRESS 상태로 반환된다")
        void success() {
            // given
            Long recommendedMissionId = 1L;
            MissionSelectRequest request = new MissionSelectRequest();
            request.setRecommendedMissionId(recommendedMissionId);

            RecommendedMissionResponse response =
                    createRecommendedMission(recommendedMissionId, "스트레칭 10분", RecommendedMissionStatus.IN_PROGRESS);

            given(missionService.startMission(principal.userId(), recommendedMissionId))
                    .willReturn(response);

            // when
            ApiResponse<RecommendedMissionResponse> result =
                    missionController.startMission(principal, request);

            // then
            assertThat(result.success()).isTrue();
            assertThat(result.data()).isNotNull();
            assertThat(result.data().getStatus()).isEqualTo(RecommendedMissionStatus.IN_PROGRESS);
            assertThat(result.data().getRecommendedMissionId()).isEqualTo(recommendedMissionId);

            then(missionService).should().startMission(principal.userId(), recommendedMissionId);
        }
    }

    @Nested
    @DisplayName("미션 결과 등록")
    class CompleteMission {

        @Test
        @DisplayName("성공 - 미션 성공 결과 등록")
        void successResult() {
            // given
            MissionResultRequest request = new MissionResultRequest();
            request.setResult(MissionResult.SUCCESS);

            MissionResultResponse response = MissionResultResponse.builder()
                    .id(1L)
                    .missionDate(LocalDate.now())
                    .result(MissionResult.SUCCESS)
                    .mission(createMissionResponse(1L, "스트레칭 10분"))
                    .build();

            given(missionService.completeMission(principal.userId(), request))
                    .willReturn(response);

            // when
            ApiResponse<MissionResultResponse> result =
                    missionController.completeMission(principal, request);

            // then
            assertThat(result.success()).isTrue();
            assertThat(result.data()).isNotNull();
            assertThat(result.data().getResult()).isEqualTo(MissionResult.SUCCESS);
            assertThat(result.data().getFailureReason()).isNull();

            then(missionService).should().completeMission(principal.userId(), request);
        }

        @Test
        @DisplayName("성공 - 미션 실패 결과 등록 (실패 사유 포함)")
        void failureResult() {
            // given
            MissionResultRequest request = new MissionResultRequest();
            request.setResult(MissionResult.FAILURE);
            request.setFailureReason("시간이 부족했습니다");

            MissionResultResponse response = MissionResultResponse.builder()
                    .id(1L)
                    .missionDate(LocalDate.now())
                    .result(MissionResult.FAILURE)
                    .failureReason("시간이 부족했습니다")
                    .mission(createMissionResponse(1L, "스트레칭 10분"))
                    .build();

            given(missionService.completeMission(principal.userId(), request))
                    .willReturn(response);

            // when
            ApiResponse<MissionResultResponse> result =
                    missionController.completeMission(principal, request);

            // then
            assertThat(result.success()).isTrue();
            assertThat(result.data().getResult()).isEqualTo(MissionResult.FAILURE);
            assertThat(result.data().getFailureReason()).isEqualTo("시간이 부족했습니다");
        }
    }

    @Nested
    @DisplayName("오늘의 미션 상태 조회")
    class GetTodayMissionStatus {

        @Test
        @DisplayName("성공 - 추천 미션이 있고 진행 중인 미션이 있는 상태")
        void successWithInProgress() {
            // given
            TodayMissionStatusResponse response = TodayMissionStatusResponse.builder()
                    .date(LocalDate.now())
                    .hasRecommendations(true)
                    .hasInProgressMission(true)
                    .hasCompletedMission(false)
                    .currentMission(createRecommendedMission(1L, "스트레칭 10분", RecommendedMissionStatus.IN_PROGRESS))
                    .build();

            given(missionService.getTodayMissionStatus(principal.userId()))
                    .willReturn(response);

            // when
            ApiResponse<TodayMissionStatusResponse> result =
                    missionController.getTodayMissionStatus(principal);

            // then
            assertThat(result.success()).isTrue();
            assertThat(result.data()).isNotNull();
            assertThat(result.data().isHasRecommendations()).isTrue();
            assertThat(result.data().isHasInProgressMission()).isTrue();
            assertThat(result.data().isHasCompletedMission()).isFalse();
            assertThat(result.data().getCurrentMission()).isNotNull();

            then(missionService).should().getTodayMissionStatus(principal.userId());
        }

        @Test
        @DisplayName("성공 - 미션이 완료된 상태")
        void successWithCompleted() {
            // given
            MissionResultResponse missionResult = MissionResultResponse.builder()
                    .id(1L)
                    .missionDate(LocalDate.now())
                    .result(MissionResult.SUCCESS)
                    .mission(createMissionResponse(1L, "스트레칭 10분"))
                    .build();

            TodayMissionStatusResponse response = TodayMissionStatusResponse.builder()
                    .date(LocalDate.now())
                    .hasRecommendations(true)
                    .hasInProgressMission(false)
                    .hasCompletedMission(true)
                    .missionResult(missionResult)
                    .build();

            given(missionService.getTodayMissionStatus(principal.userId()))
                    .willReturn(response);

            // when
            ApiResponse<TodayMissionStatusResponse> result =
                    missionController.getTodayMissionStatus(principal);

            // then
            assertThat(result.success()).isTrue();
            assertThat(result.data().isHasCompletedMission()).isTrue();
            assertThat(result.data().getMissionResult()).isNotNull();
            assertThat(result.data().getMissionResult().getResult()).isEqualTo(MissionResult.SUCCESS);
        }

        @Test
        @DisplayName("성공 - 추천 미션이 없는 초기 상태")
        void successNoRecommendations() {
            // given
            TodayMissionStatusResponse response = TodayMissionStatusResponse.builder()
                    .date(LocalDate.now())
                    .hasRecommendations(false)
                    .hasInProgressMission(false)
                    .hasCompletedMission(false)
                    .build();

            given(missionService.getTodayMissionStatus(principal.userId()))
                    .willReturn(response);

            // when
            ApiResponse<TodayMissionStatusResponse> result =
                    missionController.getTodayMissionStatus(principal);

            // then
            assertThat(result.success()).isTrue();
            assertThat(result.data().isHasRecommendations()).isFalse();
            assertThat(result.data().getCurrentMission()).isNull();
            assertThat(result.data().getMissionResult()).isNull();
        }
    }

    // ===== 헬퍼 메서드 =====

    private RecommendedMissionResponse createRecommendedMission(
            Long id, String name, RecommendedMissionStatus status) {
        return RecommendedMissionResponse.builder()
                .recommendedMissionId(id)
                .missionDate(LocalDate.now())
                .status(status)
                .mission(createMissionResponse(id, name))
                .build();
    }

    private MissionResponse createMissionResponse(Long id, String name) {
        return MissionResponse.builder()
                .id(id)
                .name(name)
                .type(MissionType.EXERCISE)
                .difficulty(2)
                .estimatedMinutes(15)
                .estimatedCalories(50)
                .build();
    }
}
