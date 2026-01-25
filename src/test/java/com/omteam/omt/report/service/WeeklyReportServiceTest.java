package com.omteam.omt.report.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import com.omteam.omt.mission.domain.MissionResult;
import com.omteam.omt.mission.repository.DailyMissionResultRepository;
import com.omteam.omt.report.domain.WeeklyAiAnalysis;
import com.omteam.omt.report.dto.WeeklyReportResponse;
import com.omteam.omt.report.repository.WeeklyAiAnalysisRepository;
import com.omteam.omt.user.domain.User;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WeeklyReportServiceTest {

    @Mock
    WeeklyAiAnalysisRepository weeklyAiAnalysisRepository;
    @Mock
    DailyMissionResultRepository missionResultRepository;

    WeeklyReportService weeklyReportService;

    final Long userId = 1L;

    @BeforeEach
    void setUp() {
        weeklyReportService = new WeeklyReportService(
                weeklyAiAnalysisRepository,
                missionResultRepository
        );
    }

    @Test
    @DisplayName("주간 리포트 조회 - AI 분석 결과 있음")
    void getWeeklyReport_with_ai_analysis() {
        // given
        LocalDate weekStartDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        WeeklyAiAnalysis analysis = createWeeklyAiAnalysis(weekStartDate);

        given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(userId, weekStartDate))
                .willReturn(Optional.of(analysis));
        given(missionResultRepository.findFailureReasonsByUserIdAndDateRange(eq(userId), eq(MissionResult.FAILURE), any(LocalDate.class)))
                .willReturn(List.of("시간 부족", "시간 부족", "피로", "날씨"));

        // when
        WeeklyReportResponse response = weeklyReportService.getWeeklyReport(userId, weekStartDate);

        // then
        assertThat(response.weekStartDate()).isEqualTo(weekStartDate);
        assertThat(response.weekEndDate()).isEqualTo(weekStartDate.plusDays(6));
        assertThat(response.aiAnalysis().summary()).isEqualTo("이번 주 잘 하셨어요!");
        assertThat(response.aiAnalysis().insight()).isEqualTo("화요일에 집중도가 높았습니다.");
        assertThat(response.aiAnalysis().recommendation()).isEqualTo("다음 주에도 화요일을 활용해보세요.");
    }

    @Test
    @DisplayName("주간 리포트 조회 - AI 분석 결과 없으면 기본 메시지")
    void getWeeklyReport_without_ai_analysis() {
        // given
        LocalDate weekStartDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(userId, weekStartDate))
                .willReturn(Optional.empty());
        given(missionResultRepository.findFailureReasonsByUserIdAndDateRange(eq(userId), eq(MissionResult.FAILURE), any(LocalDate.class)))
                .willReturn(List.of());

        // when
        WeeklyReportResponse response = weeklyReportService.getWeeklyReport(userId, weekStartDate);

        // then
        assertThat(response.aiAnalysis().summary()).contains("아직 이번 주 AI 분석 결과가 없습니다");
        assertThat(response.aiAnalysis().insight()).contains("매주 월요일에");
    }

    @Test
    @DisplayName("주간 리포트 조회 - weekStartDate null이면 이번주")
    void getWeeklyReport_null_date_defaults_to_this_week() {
        // given
        given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(eq(userId), any(LocalDate.class)))
                .willReturn(Optional.empty());
        given(missionResultRepository.findFailureReasonsByUserIdAndDateRange(eq(userId), eq(MissionResult.FAILURE), any(LocalDate.class)))
                .willReturn(List.of());

        // when
        WeeklyReportResponse response = weeklyReportService.getWeeklyReport(userId, null);

        // then
        LocalDate expectedStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        assertThat(response.weekStartDate()).isEqualTo(expectedStart);
    }

    @Test
    @DisplayName("주간 리포트 조회 - 실패 원인 순위 계산")
    void getWeeklyReport_failure_reason_ranking() {
        // given
        LocalDate weekStartDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(userId, weekStartDate))
                .willReturn(Optional.empty());
        given(missionResultRepository.findFailureReasonsByUserIdAndDateRange(eq(userId), eq(MissionResult.FAILURE), any(LocalDate.class)))
                .willReturn(List.of("시간 부족", "시간 부족", "시간 부족", "피로", "피로", "날씨"));

        // when
        WeeklyReportResponse response = weeklyReportService.getWeeklyReport(userId, weekStartDate);

        // then
        assertThat(response.topFailureReasons()).hasSize(3);
        assertThat(response.topFailureReasons().get(0).rank()).isEqualTo(1);
        assertThat(response.topFailureReasons().get(0).reason()).isEqualTo("시간 부족");
        assertThat(response.topFailureReasons().get(0).count()).isEqualTo(3);
        assertThat(response.topFailureReasons().get(1).rank()).isEqualTo(2);
        assertThat(response.topFailureReasons().get(1).reason()).isEqualTo("피로");
        assertThat(response.topFailureReasons().get(1).count()).isEqualTo(2);
    }

    @Test
    @DisplayName("주간 리포트 조회 - 실패 원인 없으면 빈 목록")
    void getWeeklyReport_no_failure_reasons() {
        // given
        LocalDate weekStartDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(userId, weekStartDate))
                .willReturn(Optional.empty());
        given(missionResultRepository.findFailureReasonsByUserIdAndDateRange(eq(userId), eq(MissionResult.FAILURE), any(LocalDate.class)))
                .willReturn(List.of());

        // when
        WeeklyReportResponse response = weeklyReportService.getWeeklyReport(userId, weekStartDate);

        // then
        assertThat(response.topFailureReasons()).isEmpty();
    }

    @Test
    @DisplayName("주간 리포트 조회 - 실패 원인 최대 5개까지만")
    void getWeeklyReport_max_5_failure_reasons() {
        // given
        LocalDate weekStartDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(userId, weekStartDate))
                .willReturn(Optional.empty());
        given(missionResultRepository.findFailureReasonsByUserIdAndDateRange(eq(userId), eq(MissionResult.FAILURE), any(LocalDate.class)))
                .willReturn(List.of("a", "a", "b", "b", "c", "c", "d", "d", "e", "e", "f", "f", "g", "g"));

        // when
        WeeklyReportResponse response = weeklyReportService.getWeeklyReport(userId, weekStartDate);

        // then
        assertThat(response.topFailureReasons()).hasSize(5);
    }

    /* ======================== */
    /* ===== Helper Zone ====== */
    /* ======================== */

    private WeeklyAiAnalysis createWeeklyAiAnalysis(LocalDate weekStartDate) {
        User user = User.builder().email("test@test.com").build();
        user.setUserId(userId);

        return WeeklyAiAnalysis.builder()
                .user(user)
                .weekStartDate(weekStartDate)
                .weekEndDate(weekStartDate.plusDays(6))
                .summary("이번 주 잘 하셨어요!")
                .insight("화요일에 집중도가 높았습니다.")
                .recommendation("다음 주에도 화요일을 활용해보세요.")
                .topFailureReasons("시간 부족,피로")
                .build();
    }
}
