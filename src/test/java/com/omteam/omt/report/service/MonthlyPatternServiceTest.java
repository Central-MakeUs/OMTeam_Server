package com.omteam.omt.report.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import com.omteam.omt.mission.domain.DailyMissionResult;
import com.omteam.omt.mission.domain.Mission;
import com.omteam.omt.mission.domain.MissionResult;
import com.omteam.omt.mission.domain.MissionType;
import com.omteam.omt.mission.repository.DailyMissionResultRepository;
import com.omteam.omt.report.dto.MonthlyPatternResponse;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MonthlyPatternServiceTest {

    @Mock
    DailyMissionResultRepository missionResultRepository;

    MonthlyPatternService monthlyPatternService;

    final Long userId = 1L;

    @BeforeEach
    void setUp() {
        monthlyPatternService = new MonthlyPatternService(missionResultRepository);
    }

    @Nested
    @DisplayName("월간 패턴 조회 테스트")
    class GetMonthlyPatternTest {

        @Test
        @DisplayName("결과가 없을 때 모든 요일의 통계가 0으로 반환됨")
        void returnsZeroStatsWhenNoResults() {
            // given
            given(missionResultRepository.findByUserUserIdAndMissionDateBetweenOrderByMissionDateDesc(
                    eq(userId), any(), any()))
                    .willReturn(List.of());

            // when
            MonthlyPatternResponse response = monthlyPatternService.getMonthlyPattern(userId);

            // then
            assertThat(response.dayOfWeekStats()).hasSize(7);
            assertThat(response.dayOfWeekStats())
                    .allMatch(s -> s.totalCount() == 0 && s.successCount() == 0 && s.failureCount() == 0);
        }

        @Test
        @DisplayName("30일 기간이 올바르게 설정됨")
        void periodIsSetTo30Days() {
            // given
            given(missionResultRepository.findByUserUserIdAndMissionDateBetweenOrderByMissionDateDesc(
                    eq(userId), any(), any()))
                    .willReturn(List.of());

            // when
            MonthlyPatternResponse response = monthlyPatternService.getMonthlyPattern(userId);

            // then
            assertThat(response.endDate()).isEqualTo(LocalDate.now());
            assertThat(response.startDate()).isEqualTo(LocalDate.now().minusDays(30));
        }

        @Test
        @DisplayName("모든 요일이 한글명과 함께 반환됨")
        void allDaysOfWeekReturnedWithKoreanName() {
            // given
            given(missionResultRepository.findByUserUserIdAndMissionDateBetweenOrderByMissionDateDesc(
                    eq(userId), any(), any()))
                    .willReturn(List.of());

            // when
            MonthlyPatternResponse response = monthlyPatternService.getMonthlyPattern(userId);

            // then
            assertThat(response.dayOfWeekStats()).hasSize(7);
            assertThat(response.dayOfWeekStats().stream()
                    .filter(s -> s.dayOfWeek() == DayOfWeek.MONDAY)
                    .findFirst().get().dayName()).isEqualTo("월요일");
            assertThat(response.dayOfWeekStats().stream()
                    .filter(s -> s.dayOfWeek() == DayOfWeek.SUNDAY)
                    .findFirst().get().dayName()).isEqualTo("일요일");
        }
    }

    @Nested
    @DisplayName("요일별 통계 계산 테스트")
    class DayOfWeekStatisticsTest {

        @Test
        @DisplayName("요일별 성공/실패 횟수가 올바르게 계산됨")
        void calculatesSuccessAndFailureCountPerDay() {
            // given
            LocalDate monday1 = LocalDate.of(2024, 1, 15); // Monday
            LocalDate monday2 = LocalDate.of(2024, 1, 22); // Monday
            LocalDate tuesday = LocalDate.of(2024, 1, 16); // Tuesday

            List<DailyMissionResult> results = List.of(
                    createMissionResult(monday1, MissionResult.SUCCESS),
                    createMissionResult(monday2, MissionResult.SUCCESS),
                    createMissionResult(tuesday, MissionResult.FAILURE)
            );

            given(missionResultRepository.findByUserUserIdAndMissionDateBetweenOrderByMissionDateDesc(
                    eq(userId), any(), any()))
                    .willReturn(results);

            // when
            MonthlyPatternResponse response = monthlyPatternService.getMonthlyPattern(userId);

            // then
            MonthlyPatternResponse.DayOfWeekStatistics mondayStats = response.dayOfWeekStats().stream()
                    .filter(s -> s.dayOfWeek() == DayOfWeek.MONDAY)
                    .findFirst().get();
            assertThat(mondayStats.successCount()).isEqualTo(2);
            assertThat(mondayStats.failureCount()).isEqualTo(0);
            assertThat(mondayStats.totalCount()).isEqualTo(2);

            MonthlyPatternResponse.DayOfWeekStatistics tuesdayStats = response.dayOfWeekStats().stream()
                    .filter(s -> s.dayOfWeek() == DayOfWeek.TUESDAY)
                    .findFirst().get();
            assertThat(tuesdayStats.successCount()).isEqualTo(0);
            assertThat(tuesdayStats.failureCount()).isEqualTo(1);
            assertThat(tuesdayStats.totalCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("요일별 성공률이 올바르게 계산됨")
        void calculatesSuccessRatePerDay() {
            // given
            LocalDate monday1 = LocalDate.of(2024, 1, 15);
            LocalDate monday2 = LocalDate.of(2024, 1, 22);
            LocalDate monday3 = LocalDate.of(2024, 1, 29);
            LocalDate monday4 = LocalDate.of(2024, 2, 5);

            List<DailyMissionResult> results = List.of(
                    createMissionResult(monday1, MissionResult.SUCCESS),
                    createMissionResult(monday2, MissionResult.SUCCESS),
                    createMissionResult(monday3, MissionResult.SUCCESS),
                    createMissionResult(monday4, MissionResult.FAILURE)
            );

            given(missionResultRepository.findByUserUserIdAndMissionDateBetweenOrderByMissionDateDesc(
                    eq(userId), any(), any()))
                    .willReturn(results);

            // when
            MonthlyPatternResponse response = monthlyPatternService.getMonthlyPattern(userId);

            // then
            MonthlyPatternResponse.DayOfWeekStatistics mondayStats = response.dayOfWeekStats().stream()
                    .filter(s -> s.dayOfWeek() == DayOfWeek.MONDAY)
                    .findFirst().get();
            assertThat(mondayStats.successRate()).isEqualTo(75.0); // 3/4 = 75%
        }
    }

    @Nested
    @DisplayName("AI 피드백 생성 테스트")
    class AiFeedbackTest {

        @Test
        @DisplayName("데이터가 없을 때 기본 피드백 반환")
        void returnsDefaultFeedbackWhenNoData() {
            // given
            given(missionResultRepository.findByUserUserIdAndMissionDateBetweenOrderByMissionDateDesc(
                    eq(userId), any(), any()))
                    .willReturn(List.of());

            // when
            MonthlyPatternResponse response = monthlyPatternService.getMonthlyPattern(userId);

            // then
            assertThat(response.aiFeedback().summary()).isEqualTo("아직 충분한 데이터가 없습니다.");
            assertThat(response.aiFeedback().recommendation()).isEqualTo("꾸준히 미션을 수행하면 더 정확한 분석을 받을 수 있어요!");
        }

        @Test
        @DisplayName("최고 성공 요일이 피드백에 포함됨")
        void includesBestDayInFeedback() {
            // given
            LocalDate monday = LocalDate.of(2024, 1, 15);
            LocalDate tuesday = LocalDate.of(2024, 1, 16);

            List<DailyMissionResult> results = List.of(
                    createMissionResult(monday, MissionResult.SUCCESS),
                    createMissionResult(tuesday, MissionResult.FAILURE)
            );

            given(missionResultRepository.findByUserUserIdAndMissionDateBetweenOrderByMissionDateDesc(
                    eq(userId), any(), any()))
                    .willReturn(results);

            // when
            MonthlyPatternResponse response = monthlyPatternService.getMonthlyPattern(userId);

            // then
            assertThat(response.aiFeedback().summary()).contains("월요일");
            assertThat(response.aiFeedback().summary()).contains("100%");
        }

        @Test
        @DisplayName("저성공률 요일이 있을 때 휴식 추천 생성")
        void recommendsRestOnLowSuccessDay() {
            // given
            LocalDate monday = LocalDate.of(2024, 1, 15);
            LocalDate tuesday1 = LocalDate.of(2024, 1, 16);
            LocalDate tuesday2 = LocalDate.of(2024, 1, 23);
            LocalDate tuesday3 = LocalDate.of(2024, 1, 30);

            List<DailyMissionResult> results = List.of(
                    createMissionResult(monday, MissionResult.SUCCESS),
                    createMissionResult(tuesday1, MissionResult.FAILURE),
                    createMissionResult(tuesday2, MissionResult.FAILURE),
                    createMissionResult(tuesday3, MissionResult.FAILURE)
            );

            given(missionResultRepository.findByUserUserIdAndMissionDateBetweenOrderByMissionDateDesc(
                    eq(userId), any(), any()))
                    .willReturn(results);

            // when
            MonthlyPatternResponse response = monthlyPatternService.getMonthlyPattern(userId);

            // then
            assertThat(response.aiFeedback().recommendation()).contains("화요일");
            assertThat(response.aiFeedback().recommendation()).contains("휴식");
            assertThat(response.aiFeedback().recommendation()).contains("월요일");
        }

        @Test
        @DisplayName("모든 요일 성공률이 높을 때 유지 추천 생성")
        void recommendsMaintenanceWhenAllDaysHaveHighSuccessRate() {
            // given
            LocalDate monday = LocalDate.of(2024, 1, 15);
            LocalDate tuesday = LocalDate.of(2024, 1, 16);

            List<DailyMissionResult> results = List.of(
                    createMissionResult(monday, MissionResult.SUCCESS),
                    createMissionResult(tuesday, MissionResult.SUCCESS)
            );

            given(missionResultRepository.findByUserUserIdAndMissionDateBetweenOrderByMissionDateDesc(
                    eq(userId), any(), any()))
                    .willReturn(results);

            // when
            MonthlyPatternResponse response = monthlyPatternService.getMonthlyPattern(userId);

            // then
            assertThat(response.aiFeedback().recommendation()).isEqualTo("현재 패턴을 유지하면서 꾸준히 진행해보세요!");
        }
    }

    @Nested
    @DisplayName("최고/최저 성공 요일 찾기 테스트")
    class BestAndWorstDayTest {

        @Test
        @DisplayName("최고 성공률 요일이 올바르게 식별됨")
        void identifiesBestPerformingDay() {
            // given
            LocalDate monday = LocalDate.of(2024, 1, 15);
            LocalDate tuesday1 = LocalDate.of(2024, 1, 16);
            LocalDate tuesday2 = LocalDate.of(2024, 1, 23);
            LocalDate wednesday = LocalDate.of(2024, 1, 17);

            List<DailyMissionResult> results = List.of(
                    createMissionResult(monday, MissionResult.SUCCESS),
                    createMissionResult(tuesday1, MissionResult.SUCCESS),
                    createMissionResult(tuesday2, MissionResult.FAILURE),
                    createMissionResult(wednesday, MissionResult.FAILURE)
            );

            given(missionResultRepository.findByUserUserIdAndMissionDateBetweenOrderByMissionDateDesc(
                    eq(userId), any(), any()))
                    .willReturn(results);

            // when
            MonthlyPatternResponse response = monthlyPatternService.getMonthlyPattern(userId);

            // then
            assertThat(response.aiFeedback().summary()).contains("월요일");
        }

        @Test
        @DisplayName("동일한 성공률일 때도 정상 동작")
        void handlesEqualSuccessRates() {
            // given
            LocalDate monday = LocalDate.of(2024, 1, 15);
            LocalDate tuesday = LocalDate.of(2024, 1, 16);

            List<DailyMissionResult> results = List.of(
                    createMissionResult(monday, MissionResult.SUCCESS),
                    createMissionResult(tuesday, MissionResult.SUCCESS)
            );

            given(missionResultRepository.findByUserUserIdAndMissionDateBetweenOrderByMissionDateDesc(
                    eq(userId), any(), any()))
                    .willReturn(results);

            // when
            MonthlyPatternResponse response = monthlyPatternService.getMonthlyPattern(userId);

            // then
            assertThat(response.aiFeedback().summary()).isNotNull();
            assertThat(response.aiFeedback().recommendation()).isNotNull();
        }
    }

    private DailyMissionResult createMissionResult(LocalDate date, MissionResult result) {
        Mission mission = Mission.builder()
                .name("테스트 미션")
                .type(MissionType.EXERCISE)
                .build();

        return DailyMissionResult.builder()
                .missionDate(date)
                .result(result)
                .mission(mission)
                .build();
    }
}
