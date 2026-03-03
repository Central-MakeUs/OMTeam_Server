package com.omteam.omt.report.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import com.omteam.omt.common.util.DateRangeUtils;
import com.omteam.omt.mission.domain.DailyMissionResult;
import com.omteam.omt.mission.domain.Mission;
import com.omteam.omt.mission.domain.MissionResult;
import com.omteam.omt.mission.domain.MissionType;
import com.omteam.omt.mission.repository.DailyMissionResultRepository;
import com.omteam.omt.report.constant.DefaultReportMessages;
import com.omteam.omt.report.domain.WeeklyAiAnalysis;
import com.omteam.omt.report.dto.MonthlyPatternResponse;
import com.omteam.omt.report.dto.ReportDataStatus;
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
class MonthlyPatternServiceTest {

    @Mock
    DailyMissionResultRepository missionResultRepository;

    @Mock
    WeeklyAiAnalysisRepository weeklyAiAnalysisRepository;

    MonthlyPatternService monthlyPatternService;

    final Long userId = 1L;

    @BeforeEach
    void setUp() {
        monthlyPatternService = new MonthlyPatternService(
                missionResultRepository,
                weeklyAiAnalysisRepository
        );
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
            given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(eq(userId), any()))
                    .willReturn(Optional.empty());

            // when
            MonthlyPatternResponse response = monthlyPatternService.getMonthlyPattern(userId, null, null, null);

            // then
            assertThat(response.dayOfWeekStats()).hasSize(7);
            assertThat(response.dayOfWeekStats())
                    .allMatch(s -> s.totalCount() == 0 && s.successCount() == 0 && s.failureCount() == 0);
            assertThat(response.dataStatus()).isEqualTo(ReportDataStatus.NO_DATA);
        }

        @Test
        @DisplayName("파라미터 없이 호출 시 이번 주 월요일 기준으로 30일 기간이 설정됨")
        void periodIsSetTo30Days() {
            // given
            given(missionResultRepository.findByUserUserIdAndMissionDateBetweenOrderByMissionDateDesc(
                    eq(userId), any(), any()))
                    .willReturn(List.of());
            given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(eq(userId), any()))
                    .willReturn(Optional.empty());

            // when
            MonthlyPatternResponse response = monthlyPatternService.getMonthlyPattern(userId, null, null, null);

            // then
            LocalDate expectedEnd = DateRangeUtils.getWeekStartDate(LocalDate.now());
            assertThat(response.endDate()).isEqualTo(expectedEnd);
            assertThat(response.startDate()).isEqualTo(expectedEnd.minusDays(30));
        }

        @Test
        @DisplayName("모든 요일이 한글명과 함께 반환됨")
        void allDaysOfWeekReturnedWithKoreanName() {
            // given
            given(missionResultRepository.findByUserUserIdAndMissionDateBetweenOrderByMissionDateDesc(
                    eq(userId), any(), any()))
                    .willReturn(List.of());
            given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(eq(userId), any()))
                    .willReturn(Optional.empty());

            // when
            MonthlyPatternResponse response = monthlyPatternService.getMonthlyPattern(userId, null, null, null);

            // then
            assertThat(response.dayOfWeekStats()).hasSize(7);
            assertThat(response.dayOfWeekStats().stream()
                    .filter(s -> s.dayOfWeek() == DayOfWeek.MONDAY)
                    .findFirst().get().dayName()).isEqualTo("월요일");
            assertThat(response.dayOfWeekStats().stream()
                    .filter(s -> s.dayOfWeek() == DayOfWeek.SUNDAY)
                    .findFirst().get().dayName()).isEqualTo("일요일");
        }

        @Test
        @DisplayName("year/month/weekOfMonth 지정 시 해당 주 월요일 기준으로 30일 기간이 설정됨")
        void periodIsSetBasedOnSpecifiedWeek() {
            // given: 2024년 1월 3번째 주 월요일 = 2024-01-15
            given(missionResultRepository.findByUserUserIdAndMissionDateBetweenOrderByMissionDateDesc(
                    eq(userId), any(), any()))
                    .willReturn(List.of());
            given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(eq(userId), any()))
                    .willReturn(Optional.empty());

            // when
            MonthlyPatternResponse response = monthlyPatternService.getMonthlyPattern(userId, 2024, 1, 3);

            // then
            LocalDate expectedEnd = LocalDate.of(2024, 1, 15);
            assertThat(response.endDate()).isEqualTo(expectedEnd);
            assertThat(response.startDate()).isEqualTo(expectedEnd.minusDays(30));
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
            given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(eq(userId), any()))
                    .willReturn(Optional.empty());

            // when
            MonthlyPatternResponse response = monthlyPatternService.getMonthlyPattern(userId, null, null, null);

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
            given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(eq(userId), any()))
                    .willReturn(Optional.empty());

            // when
            MonthlyPatternResponse response = monthlyPatternService.getMonthlyPattern(userId, null, null, null);

            // then
            MonthlyPatternResponse.DayOfWeekStatistics mondayStats = response.dayOfWeekStats().stream()
                    .filter(s -> s.dayOfWeek() == DayOfWeek.MONDAY)
                    .findFirst().get();
            assertThat(mondayStats.successRate()).isEqualTo(75.0); // 3/4 = 75%
        }
    }

    @Nested
    @DisplayName("AI 피드백 조회 테스트")
    class AiFeedbackTest {

        @Test
        @DisplayName("AI 분석 결과가 없을 때 NO_DATA 기본 메시지 반환")
        void returnsNullFeedbackWhenNoAnalysis() {
            // given
            given(missionResultRepository.findByUserUserIdAndMissionDateBetweenOrderByMissionDateDesc(
                    eq(userId), any(), any()))
                    .willReturn(List.of());
            given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(eq(userId), any()))
                    .willReturn(Optional.empty());

            // when
            MonthlyPatternResponse response = monthlyPatternService.getMonthlyPattern(userId, null, null, null);

            // then
            assertThat(response.aiFeedback().dayOfWeekFeedbackTitle())
                    .isEqualTo(DefaultReportMessages.MONTHLY_DAY_NO_DATA_TITLE);
            assertThat(response.aiFeedback().dayOfWeekFeedbackContent())
                    .isEqualTo(DefaultReportMessages.MONTHLY_DAY_NO_DATA_CONTENT);
            assertThat(response.aiFeedback().isDefault()).isTrue();
            assertThat(response.dataStatus()).isEqualTo(ReportDataStatus.NO_DATA);
        }

        @Test
        @DisplayName("AI 분석 결과가 있을 때 피드백 반환")
        void returnsFeedbackWhenAnalysisExists() {
            // given
            User user = User.builder().userId(userId).build();
            WeeklyAiAnalysis analysis = WeeklyAiAnalysis.builder()
                    .user(user)
                    .weekStartDate(LocalDate.now())
                    .dayOfWeekFeedbackTitle("화요일에 집중해보세요")
                    .dayOfWeekFeedbackContent("화요일에 미션 수행률이 낮았습니다.")
                    .build();

            given(missionResultRepository.findByUserUserIdAndMissionDateBetweenOrderByMissionDateDesc(
                    eq(userId), any(), any()))
                    .willReturn(List.of());
            given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(eq(userId), any()))
                    .willReturn(Optional.of(analysis));

            // when
            MonthlyPatternResponse response = monthlyPatternService.getMonthlyPattern(userId, null, null, null);

            // then
            assertThat(response.aiFeedback().dayOfWeekFeedbackTitle()).isEqualTo("화요일에 집중해보세요");
            assertThat(response.aiFeedback().dayOfWeekFeedbackContent()).isEqualTo("화요일에 미션 수행률이 낮았습니다.");
            assertThat(response.aiFeedback().isDefault()).isFalse();
            assertThat(response.dataStatus()).isEqualTo(ReportDataStatus.READY);
        }

        @Test
        @DisplayName("미션 데이터는 있지만 AI 분석이 없을 때 PENDING_ANALYSIS 상태와 기본 메시지 반환")
        void returnsPendingStatusWhenHasMissionButNoAnalysis() {
            // given
            LocalDate monday = LocalDate.of(2024, 1, 15);
            List<DailyMissionResult> results = List.of(
                    createMissionResult(monday, MissionResult.SUCCESS)
            );

            given(missionResultRepository.findByUserUserIdAndMissionDateBetweenOrderByMissionDateDesc(
                    eq(userId), any(), any()))
                    .willReturn(results);
            given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(eq(userId), any()))
                    .willReturn(Optional.empty());

            // when
            MonthlyPatternResponse response = monthlyPatternService.getMonthlyPattern(userId, null, null, null);

            // then
            assertThat(response.aiFeedback().dayOfWeekFeedbackTitle())
                    .isEqualTo(DefaultReportMessages.MONTHLY_DAY_PENDING_TITLE);
            assertThat(response.aiFeedback().dayOfWeekFeedbackContent())
                    .isEqualTo(DefaultReportMessages.MONTHLY_DAY_PENDING_CONTENT);
            assertThat(response.aiFeedback().isDefault()).isTrue();
            assertThat(response.dataStatus()).isEqualTo(ReportDataStatus.PENDING_ANALYSIS);
        }

        @Test
        @DisplayName("미션 데이터도 없고 AI 분석도 없을 때 NO_DATA 상태와 기본 메시지 반환")
        void returnsNoDataStatusWhenNoMissionAndNoAnalysis() {
            // given
            given(missionResultRepository.findByUserUserIdAndMissionDateBetweenOrderByMissionDateDesc(
                    eq(userId), any(), any()))
                    .willReturn(List.of());
            given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(eq(userId), any()))
                    .willReturn(Optional.empty());

            // when
            MonthlyPatternResponse response = monthlyPatternService.getMonthlyPattern(userId, null, null, null);

            // then
            assertThat(response.aiFeedback().dayOfWeekFeedbackTitle())
                    .isEqualTo(DefaultReportMessages.MONTHLY_DAY_NO_DATA_TITLE);
            assertThat(response.aiFeedback().dayOfWeekFeedbackContent())
                    .isEqualTo(DefaultReportMessages.MONTHLY_DAY_NO_DATA_CONTENT);
            assertThat(response.aiFeedback().isDefault()).isTrue();
            assertThat(response.dataStatus()).isEqualTo(ReportDataStatus.NO_DATA);
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
