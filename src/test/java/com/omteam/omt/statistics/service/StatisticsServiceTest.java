package com.omteam.omt.statistics.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import com.omteam.omt.mission.domain.DailyMissionResult;
import com.omteam.omt.mission.domain.Mission;
import com.omteam.omt.mission.domain.MissionResult;
import com.omteam.omt.mission.domain.MissionType;
import com.omteam.omt.mission.repository.DailyMissionResultRepository;
import com.omteam.omt.statistics.dto.MissionTypeStatisticsResponse;
import com.omteam.omt.statistics.dto.MonthlyPatternResponse;
import com.omteam.omt.statistics.dto.WeeklyStatisticsResponse;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock
    DailyMissionResultRepository missionResultRepository;

    StatisticsService statisticsService;

    final Long userId = 1L;

    @BeforeEach
    void setUp() {
        statisticsService = new StatisticsService(missionResultRepository);
    }

    @Test
    @DisplayName("주간 통계 조회 - 성공률 계산")
    void getWeeklyStatistics_success_rate() {
        // given
        LocalDate today = LocalDate.now();
        LocalDate thisWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        List<DailyMissionResult> thisWeekResults = List.of(
                createMissionResult(thisWeekStart, MissionResult.SUCCESS, MissionType.EXERCISE),
                createMissionResult(thisWeekStart.plusDays(1), MissionResult.SUCCESS, MissionType.EXERCISE),
                createMissionResult(thisWeekStart.plusDays(2), MissionResult.FAILURE, MissionType.DIET)
        );

        given(missionResultRepository.findByUserUserIdAndMissionDateBetweenOrderByMissionDateDesc(
                eq(userId), eq(thisWeekStart), any(LocalDate.class)))
                .willReturn(thisWeekResults);
        given(missionResultRepository.findByUserUserIdAndMissionDateBetweenOrderByMissionDateDesc(
                eq(userId), eq(thisWeekStart.minusDays(7)), eq(thisWeekStart.minusDays(1))))
                .willReturn(List.of());

        // when
        WeeklyStatisticsResponse response = statisticsService.getWeeklyStatistics(userId);

        // then
        assertThat(response.thisWeek().successCount()).isEqualTo(2);
        assertThat(response.thisWeek().failureCount()).isEqualTo(1);
        assertThat(response.thisWeek().totalCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("주간 통계 조회 - 요일별 결과 포함")
    void getWeeklyStatistics_daily_results() {
        // given
        LocalDate today = LocalDate.now();
        LocalDate thisWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        List<DailyMissionResult> thisWeekResults = List.of(
                createMissionResult(thisWeekStart, MissionResult.SUCCESS, MissionType.EXERCISE)
        );

        given(missionResultRepository.findByUserUserIdAndMissionDateBetweenOrderByMissionDateDesc(
                eq(userId), eq(thisWeekStart), any(LocalDate.class)))
                .willReturn(thisWeekResults);
        given(missionResultRepository.findByUserUserIdAndMissionDateBetweenOrderByMissionDateDesc(
                eq(userId), eq(thisWeekStart.minusDays(7)), eq(thisWeekStart.minusDays(1))))
                .willReturn(List.of());

        // when
        WeeklyStatisticsResponse response = statisticsService.getWeeklyStatistics(userId);

        // then
        assertThat(response.dailyResults()).isNotEmpty();
        assertThat(response.dailyResults().get(0).dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
    }

    @Test
    @DisplayName("미션 종류별 통계 조회")
    void getMissionTypeStatistics() {
        // given
        List<DailyMissionResult> allResults = List.of(
                createMissionResult(LocalDate.now().minusDays(1), MissionResult.SUCCESS, MissionType.EXERCISE),
                createMissionResult(LocalDate.now().minusDays(2), MissionResult.SUCCESS, MissionType.EXERCISE),
                createMissionResult(LocalDate.now().minusDays(3), MissionResult.FAILURE, MissionType.EXERCISE),
                createMissionResult(LocalDate.now().minusDays(4), MissionResult.SUCCESS, MissionType.DIET),
                createMissionResult(LocalDate.now().minusDays(5), MissionResult.FAILURE, MissionType.DIET)
        );

        given(missionResultRepository.findByUserUserIdOrderByMissionDateDesc(userId))
                .willReturn(allResults);

        // when
        MissionTypeStatisticsResponse response = statisticsService.getMissionTypeStatistics(userId);

        // then
        assertThat(response.totalSuccessCount()).isEqualTo(3);
        assertThat(response.byType()).hasSize(2);

        var exerciseStats = response.byType().stream()
                .filter(s -> s.type() == MissionType.EXERCISE)
                .findFirst()
                .orElseThrow();
        assertThat(exerciseStats.successCount()).isEqualTo(2);
        assertThat(exerciseStats.failureCount()).isEqualTo(1);
        assertThat(exerciseStats.successRate()).isEqualTo(66.7);

        var dietStats = response.byType().stream()
                .filter(s -> s.type() == MissionType.DIET)
                .findFirst()
                .orElseThrow();
        assertThat(dietStats.successCount()).isEqualTo(1);
        assertThat(dietStats.failureCount()).isEqualTo(1);
        assertThat(dietStats.successRate()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("미션 종류별 통계 조회 - 데이터 없음")
    void getMissionTypeStatistics_empty() {
        // given
        given(missionResultRepository.findByUserUserIdOrderByMissionDateDesc(userId))
                .willReturn(List.of());

        // when
        MissionTypeStatisticsResponse response = statisticsService.getMissionTypeStatistics(userId);

        // then
        assertThat(response.totalSuccessCount()).isEqualTo(0);
        assertThat(response.byType()).hasSize(2);
        assertThat(response.byType().get(0).totalCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("월간 패턴 분석 - 요일별 통계")
    void getMonthlyPattern_day_of_week_stats() {
        // given
        LocalDate today = LocalDate.now();
        // 월요일에 성공, 화요일에 실패
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate tuesday = monday.plusDays(1);

        List<DailyMissionResult> results = List.of(
                createMissionResult(monday, MissionResult.SUCCESS, MissionType.EXERCISE),
                createMissionResult(monday.minusDays(7), MissionResult.SUCCESS, MissionType.EXERCISE),
                createMissionResult(tuesday, MissionResult.FAILURE, MissionType.DIET)
        );

        given(missionResultRepository.findByUserUserIdAndMissionDateBetweenOrderByMissionDateDesc(
                eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(results);

        // when
        MonthlyPatternResponse response = statisticsService.getMonthlyPattern(userId);

        // then
        assertThat(response.dayOfWeekStats()).hasSize(7);

        var mondayStats = response.dayOfWeekStats().stream()
                .filter(s -> s.dayOfWeek() == DayOfWeek.MONDAY)
                .findFirst()
                .orElseThrow();
        assertThat(mondayStats.successCount()).isEqualTo(2);
        assertThat(mondayStats.successRate()).isEqualTo(100.0);
        assertThat(mondayStats.dayName()).isEqualTo("월요일");

        var tuesdayStats = response.dayOfWeekStats().stream()
                .filter(s -> s.dayOfWeek() == DayOfWeek.TUESDAY)
                .findFirst()
                .orElseThrow();
        assertThat(tuesdayStats.failureCount()).isEqualTo(1);
        assertThat(tuesdayStats.successRate()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("월간 패턴 분석 - AI 피드백 생성")
    void getMonthlyPattern_ai_feedback() {
        // given
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        List<DailyMissionResult> results = List.of(
                createMissionResult(monday, MissionResult.SUCCESS, MissionType.EXERCISE),
                createMissionResult(monday.minusDays(7), MissionResult.SUCCESS, MissionType.EXERCISE)
        );

        given(missionResultRepository.findByUserUserIdAndMissionDateBetweenOrderByMissionDateDesc(
                eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(results);

        // when
        MonthlyPatternResponse response = statisticsService.getMonthlyPattern(userId);

        // then
        assertThat(response.aiFeedback()).isNotNull();
        assertThat(response.aiFeedback().summary()).contains("월요일");
        assertThat(response.aiFeedback().recommendation()).isNotBlank();
    }

    @Test
    @DisplayName("월간 패턴 분석 - 데이터 없으면 기본 피드백")
    void getMonthlyPattern_empty_default_feedback() {
        // given
        given(missionResultRepository.findByUserUserIdAndMissionDateBetweenOrderByMissionDateDesc(
                eq(userId), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(List.of());

        // when
        MonthlyPatternResponse response = statisticsService.getMonthlyPattern(userId);

        // then
        assertThat(response.aiFeedback().summary()).contains("충분한 데이터가 없습니다");
    }

    /* ======================== */
    /* ===== Helper Zone ====== */
    /* ======================== */

    private DailyMissionResult createMissionResult(LocalDate date, MissionResult result, MissionType type) {
        Mission mission = Mission.builder()
                .name("테스트 미션")
                .type(type)
                .difficulty(1)
                .estimatedMinutes(30)
                .estimatedCalories(100)
                .build();

        return DailyMissionResult.builder()
                .missionDate(date)
                .result(result)
                .mission(mission)
                .build();
    }
}
