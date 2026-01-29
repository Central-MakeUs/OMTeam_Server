package com.omteam.omt.report.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import com.omteam.omt.mission.domain.DailyMissionResult;
import com.omteam.omt.mission.domain.Mission;
import com.omteam.omt.mission.domain.MissionResult;
import com.omteam.omt.mission.domain.MissionType;
import com.omteam.omt.mission.repository.DailyMissionResultRepository;
import com.omteam.omt.report.domain.WeeklyAiAnalysis;
import com.omteam.omt.report.dto.WeeklyReportResponse;
import com.omteam.omt.report.dto.WeeklyReportResponse.DailyStatus;
import com.omteam.omt.report.repository.WeeklyAiAnalysisRepository;
import com.omteam.omt.user.domain.User;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WeeklyReportServiceTest {

    @Mock
    DailyMissionResultRepository missionResultRepository;

    @Mock
    WeeklyAiAnalysisRepository weeklyAiAnalysisRepository;

    WeeklyReportService weeklyReportService;

    final Long userId = 1L;
    final LocalDate monday = LocalDate.of(2024, 1, 15); // Monday

    @BeforeEach
    void setUp() {
        weeklyReportService = new WeeklyReportService(
                missionResultRepository,
                weeklyAiAnalysisRepository
        );
    }

    @Nested
    @DisplayName("성공률 계산 테스트")
    class SuccessRateTest {

        @Test
        @DisplayName("이번 주 성공률 계산 - 7일 중 5일 성공")
        void calculateThisWeekSuccessRate() {
            // given
            List<DailyMissionResult> results = List.of(
                    createMissionResult(monday, MissionResult.SUCCESS),
                    createMissionResult(monday.plusDays(1), MissionResult.SUCCESS),
                    createMissionResult(monday.plusDays(2), MissionResult.SUCCESS),
                    createMissionResult(monday.plusDays(3), MissionResult.FAILURE),
                    createMissionResult(monday.plusDays(4), MissionResult.SUCCESS),
                    createMissionResult(monday.plusDays(5), MissionResult.FAILURE),
                    createMissionResult(monday.plusDays(6), MissionResult.SUCCESS)
            );

            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(
                    eq(userId), eq(monday), eq(monday.plusDays(6))))
                    .willReturn(results);
            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(
                    eq(userId), eq(monday.minusDays(7)), eq(monday.minusDays(1))))
                    .willReturn(List.of());
            given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(userId, monday))
                    .willReturn(Optional.empty());

            // when
            WeeklyReportResponse response = weeklyReportService.getWeeklyReport(userId, monday);

            // then
            assertThat(response.thisWeekSuccessRate()).isEqualTo(71.4); // 5/7 = 71.4%
        }

        @Test
        @DisplayName("지난 주 성공률 계산")
        void calculateLastWeekSuccessRate() {
            // given
            LocalDate lastWeekMonday = monday.minusDays(7);
            List<DailyMissionResult> lastWeekResults = List.of(
                    createMissionResult(lastWeekMonday, MissionResult.SUCCESS),
                    createMissionResult(lastWeekMonday.plusDays(1), MissionResult.SUCCESS),
                    createMissionResult(lastWeekMonday.plusDays(2), MissionResult.SUCCESS),
                    createMissionResult(lastWeekMonday.plusDays(3), MissionResult.SUCCESS),
                    createMissionResult(lastWeekMonday.plusDays(4), MissionResult.FAILURE),
                    createMissionResult(lastWeekMonday.plusDays(5), MissionResult.FAILURE),
                    createMissionResult(lastWeekMonday.plusDays(6), MissionResult.FAILURE)
            );

            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(
                    eq(userId), eq(monday), eq(monday.plusDays(6))))
                    .willReturn(List.of());
            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(
                    eq(userId), eq(lastWeekMonday), eq(monday.minusDays(1))))
                    .willReturn(lastWeekResults);
            given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(userId, monday))
                    .willReturn(Optional.empty());

            // when
            WeeklyReportResponse response = weeklyReportService.getWeeklyReport(userId, monday);

            // then
            assertThat(response.lastWeekSuccessRate()).isEqualTo(57.1); // 4/7 = 57.1%
        }

        @Test
        @DisplayName("결과가 없을 때 성공률 0%")
        void zeroSuccessRateWhenNoResults() {
            // given
            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(
                    anyLong(), any(), any()))
                    .willReturn(List.of());
            given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(anyLong(), any()))
                    .willReturn(Optional.empty());

            // when
            WeeklyReportResponse response = weeklyReportService.getWeeklyReport(userId, monday);

            // then
            assertThat(response.thisWeekSuccessRate()).isEqualTo(0.0);
            assertThat(response.lastWeekSuccessRate()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("요일별 결과 조회 테스트")
    class DailyResultsTest {

        @Test
        @DisplayName("요일별 결과 목록 생성 - 성공, 실패, 미수행 포함")
        void buildDailyResults() {
            // given
            List<DailyMissionResult> results = List.of(
                    createMissionResult(monday, MissionResult.SUCCESS),
                    createMissionResult(monday.plusDays(2), MissionResult.FAILURE)
            );

            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(
                    eq(userId), eq(monday), eq(monday.plusDays(6))))
                    .willReturn(results);
            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(
                    eq(userId), eq(monday.minusDays(7)), eq(monday.minusDays(1))))
                    .willReturn(List.of());
            given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(userId, monday))
                    .willReturn(Optional.empty());

            // when
            WeeklyReportResponse response = weeklyReportService.getWeeklyReport(userId, monday);

            // then
            assertThat(response.dailyResults()).isNotEmpty();
            assertThat(response.dailyResults().get(0).status()).isEqualTo(DailyStatus.SUCCESS);
            assertThat(response.dailyResults().get(0).dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        }
    }

    @Nested
    @DisplayName("타입별 성공횟수 테스트")
    class TypeSuccessCountTest {

        @Test
        @DisplayName("운동 타입 3회 성공, 식단 타입 2회 성공")
        void calculateTypeSuccessCounts() {
            // given
            List<DailyMissionResult> results = List.of(
                    createMissionResult(monday, MissionResult.SUCCESS, MissionType.EXERCISE),
                    createMissionResult(monday.plusDays(1), MissionResult.SUCCESS, MissionType.EXERCISE),
                    createMissionResult(monday.plusDays(2), MissionResult.SUCCESS, MissionType.EXERCISE),
                    createMissionResult(monday.plusDays(3), MissionResult.SUCCESS, MissionType.DIET),
                    createMissionResult(monday.plusDays(4), MissionResult.SUCCESS, MissionType.DIET),
                    createMissionResult(monday.plusDays(5), MissionResult.FAILURE, MissionType.EXERCISE)
            );

            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(
                    eq(userId), eq(monday), eq(monday.plusDays(6))))
                    .willReturn(results);
            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(
                    eq(userId), eq(monday.minusDays(7)), eq(monday.minusDays(1))))
                    .willReturn(List.of());
            given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(userId, monday))
                    .willReturn(Optional.empty());

            // when
            WeeklyReportResponse response = weeklyReportService.getWeeklyReport(userId, monday);

            // then
            assertThat(response.typeSuccessCounts()).hasSize(2);
            assertThat(response.typeSuccessCounts().stream()
                    .filter(t -> t.type() == MissionType.EXERCISE)
                    .findFirst().get().successCount()).isEqualTo(3);
            assertThat(response.typeSuccessCounts().stream()
                    .filter(t -> t.type() == MissionType.DIET)
                    .findFirst().get().successCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("모든 MissionType이 포함되며 각 타입의 displayName이 올바르게 설정됨")
        void allMissionTypesIncluded() {
            // given
            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(
                    eq(userId), eq(monday), eq(monday.plusDays(6))))
                    .willReturn(List.of());
            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(
                    eq(userId), eq(monday.minusDays(7)), eq(monday.minusDays(1))))
                    .willReturn(List.of());
            given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(userId, monday))
                    .willReturn(Optional.empty());

            // when
            WeeklyReportResponse response = weeklyReportService.getWeeklyReport(userId, monday);

            // then
            assertThat(response.typeSuccessCounts()).hasSize(MissionType.values().length);
            for (MissionType type : MissionType.values()) {
                assertThat(response.typeSuccessCounts()).anySatisfy(count -> {
                    assertThat(count.type()).isEqualTo(type);
                    assertThat(count.typeName()).isEqualTo(type.getDisplayName());
                });
            }
        }

        @Test
        @DisplayName("특정 타입만 성공 기록이 있어도 모든 타입이 반환됨")
        void allTypesReturnedEvenWhenOnlyOneTypeHasSuccess() {
            // given
            List<DailyMissionResult> results = List.of(
                    createMissionResult(monday, MissionResult.SUCCESS, MissionType.EXERCISE),
                    createMissionResult(monday.plusDays(1), MissionResult.SUCCESS, MissionType.EXERCISE)
            );

            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(
                    eq(userId), eq(monday), eq(monday.plusDays(6))))
                    .willReturn(results);
            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(
                    eq(userId), eq(monday.minusDays(7)), eq(monday.minusDays(1))))
                    .willReturn(List.of());
            given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(userId, monday))
                    .willReturn(Optional.empty());

            // when
            WeeklyReportResponse response = weeklyReportService.getWeeklyReport(userId, monday);

            // then
            assertThat(response.typeSuccessCounts()).hasSize(MissionType.values().length);
            assertThat(response.typeSuccessCounts().stream()
                    .filter(t -> t.type() == MissionType.EXERCISE)
                    .findFirst().get().successCount()).isEqualTo(2);
            assertThat(response.typeSuccessCounts().stream()
                    .filter(t -> t.type() == MissionType.DIET)
                    .findFirst().get().successCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("실패한 미션은 성공 횟수에 포함되지 않음")
        void failedMissionsNotCountedInSuccessCount() {
            // given
            List<DailyMissionResult> results = List.of(
                    createMissionResult(monday, MissionResult.SUCCESS, MissionType.EXERCISE),
                    createMissionResult(monday.plusDays(1), MissionResult.FAILURE, MissionType.EXERCISE),
                    createMissionResult(monday.plusDays(2), MissionResult.FAILURE, MissionType.DIET),
                    createMissionResult(monday.plusDays(3), MissionResult.SUCCESS, MissionType.DIET)
            );

            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(
                    eq(userId), eq(monday), eq(monday.plusDays(6))))
                    .willReturn(results);
            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(
                    eq(userId), eq(monday.minusDays(7)), eq(monday.minusDays(1))))
                    .willReturn(List.of());
            given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(userId, monday))
                    .willReturn(Optional.empty());

            // when
            WeeklyReportResponse response = weeklyReportService.getWeeklyReport(userId, monday);

            // then
            assertThat(response.typeSuccessCounts())
                    .anySatisfy(count -> {
                        assertThat(count.type()).isEqualTo(MissionType.EXERCISE);
                        assertThat(count.successCount()).isEqualTo(1);
                    })
                    .anySatisfy(count -> {
                        assertThat(count.type()).isEqualTo(MissionType.DIET);
                        assertThat(count.successCount()).isEqualTo(1);
                    });
        }
    }

    @Nested
    @DisplayName("실패 원인 순위 테스트")
    class FailureReasonRankTest {

        @Test
        @DisplayName("실패 원인 순위 상위 3개 반환")
        void getTopFailureReasons() {
            // given
            List<DailyMissionResult> results = List.of(
                    createMissionResultWithReason(monday, "시간 부족"),
                    createMissionResultWithReason(monday.plusDays(1), "시간 부족"),
                    createMissionResultWithReason(monday.plusDays(2), "시간 부족"),
                    createMissionResultWithReason(monday.plusDays(3), "피로"),
                    createMissionResultWithReason(monday.plusDays(4), "피로"),
                    createMissionResultWithReason(monday.plusDays(5), "날씨")
            );

            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(
                    eq(userId), eq(monday), eq(monday.plusDays(6))))
                    .willReturn(results);
            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(
                    eq(userId), eq(monday.minusDays(7)), eq(monday.minusDays(1))))
                    .willReturn(List.of());
            given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(userId, monday))
                    .willReturn(Optional.empty());

            // when
            WeeklyReportResponse response = weeklyReportService.getWeeklyReport(userId, monday);

            // then
            assertThat(response.topFailureReasons()).hasSize(3);
            assertThat(response.topFailureReasons().get(0).rank()).isEqualTo(1);
            assertThat(response.topFailureReasons().get(0).reason()).isEqualTo("시간 부족");
            assertThat(response.topFailureReasons().get(0).count()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("AI 피드백 조회 테스트")
    class AiFeedbackTest {

        @Test
        @DisplayName("AI 분석 결과가 존재할 때")
        void getAiFeedbackWhenExists() {
            // given
            User user = User.builder().userId(userId).build();
            WeeklyAiAnalysis analysis = WeeklyAiAnalysis.builder()
                    .user(user)
                    .weekStartDate(monday)
                    .mainFailureReason("시간 부족")
                    .overallFeedback("이번 주 피드백입니다.")
                    .build();

            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(
                    anyLong(), any(), any()))
                    .willReturn(List.of());
            given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(userId, monday))
                    .willReturn(Optional.of(analysis));

            // when
            WeeklyReportResponse response = weeklyReportService.getWeeklyReport(userId, monday);

            // then
            assertThat(response.aiFeedback().mainFailureReason()).isEqualTo("시간 부족");
            assertThat(response.aiFeedback().overallFeedback()).isEqualTo("이번 주 피드백입니다.");
        }

        @Test
        @DisplayName("AI 분석 결과가 없을 때 기본 메시지 반환")
        void getDefaultMessageWhenNoAiFeedback() {
            // given
            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(
                    anyLong(), any(), any()))
                    .willReturn(List.of());
            given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(userId, monday))
                    .willReturn(Optional.empty());

            // when
            WeeklyReportResponse response = weeklyReportService.getWeeklyReport(userId, monday);

            // then
            assertThat(response.aiFeedback().mainFailureReason()).isNull();
            assertThat(response.aiFeedback().overallFeedback()).isEqualTo("아직 AI 분석 결과가 생성되지 않았습니다.");
        }
    }

    @Nested
    @DisplayName("주간 시작일 계산 테스트")
    class WeekStartDateTest {

        @Test
        @DisplayName("weekStartDate가 null일 때 이번 주 월요일 반환")
        void resolveWeekStartDateWhenNull() {
            // given
            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(
                    anyLong(), any(), any()))
                    .willReturn(List.of());
            given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(anyLong(), any()))
                    .willReturn(Optional.empty());

            // when
            WeeklyReportResponse response = weeklyReportService.getWeeklyReport(userId, null);

            // then
            assertThat(response.weekStartDate().getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        }

        @Test
        @DisplayName("weekStartDate가 월요일이 아닐 때 해당 주 월요일로 조정")
        void resolveWeekStartDateWhenNotMonday() {
            // given
            LocalDate wednesday = LocalDate.of(2024, 1, 17); // Wednesday

            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(
                    anyLong(), any(), any()))
                    .willReturn(List.of());
            given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(anyLong(), any()))
                    .willReturn(Optional.empty());

            // when
            WeeklyReportResponse response = weeklyReportService.getWeeklyReport(userId, wednesday);

            // then
            assertThat(response.weekStartDate()).isEqualTo(monday);
            assertThat(response.weekEndDate()).isEqualTo(monday.plusDays(6));
        }
    }

    private DailyMissionResult createMissionResult(LocalDate date, MissionResult result) {
        return createMissionResult(date, result, MissionType.EXERCISE);
    }

    private DailyMissionResult createMissionResult(LocalDate date, MissionResult result, MissionType type) {
        Mission mission = Mission.builder()
                .name("테스트 미션")
                .type(type)
                .build();

        return DailyMissionResult.builder()
                .missionDate(date)
                .result(result)
                .mission(mission)
                .build();
    }

    private DailyMissionResult createMissionResultWithReason(LocalDate date, String failureReason) {
        Mission mission = Mission.builder()
                .name("테스트 미션")
                .type(MissionType.EXERCISE)
                .build();

        return DailyMissionResult.builder()
                .missionDate(date)
                .result(MissionResult.FAILURE)
                .failureReason(failureReason)
                .mission(mission)
                .build();
    }
}
