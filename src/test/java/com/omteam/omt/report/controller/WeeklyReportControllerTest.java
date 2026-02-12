package com.omteam.omt.report.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import com.omteam.omt.common.response.ApiResponse;
import com.omteam.omt.report.constant.DefaultReportMessages;
import com.omteam.omt.report.dto.DailyFeedbackResponse;
import com.omteam.omt.report.dto.ReportDataStatus;
import com.omteam.omt.report.service.DailyAnalysisService;
import com.omteam.omt.report.service.MonthlyPatternService;
import com.omteam.omt.report.service.WeeklyReportService;
import com.omteam.omt.security.principal.UserPrincipal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WeeklyReportControllerTest {

    @Mock
    WeeklyReportService weeklyReportService;

    @Mock
    MonthlyPatternService monthlyPatternService;

    @Mock
    DailyAnalysisService dailyAnalysisService;

    @InjectMocks
    WeeklyReportController weeklyReportController;

    UserPrincipal principal;

    @BeforeEach
    void setUp() {
        principal = new UserPrincipal(1L);
    }

    @Test
    @DisplayName("getDailyFeedback_WithDate_Success - 날짜 파라미터 있을 때 성공")
    void getDailyFeedback_WithDate_Success() {
        // given
        LocalDate targetDate = LocalDate.of(2024, 1, 15);
        DailyFeedbackResponse response = createDailyFeedbackResponse(targetDate);

        given(dailyAnalysisService.getDailyFeedback(principal.userId(), targetDate))
                .willReturn(response);

        // when
        ApiResponse<DailyFeedbackResponse> result =
                weeklyReportController.getDailyFeedback(principal, targetDate);

        // then
        assertThat(result.success()).isTrue();
        assertThat(result.data()).isNotNull();
        assertThat(result.data().targetDate()).isEqualTo(targetDate);
        assertThat(result.data().feedbackText()).isEqualTo("오늘도 열심히 운동하셨네요!");

        then(dailyAnalysisService).should().getDailyFeedback(principal.userId(), targetDate);
    }

    @Test
    @DisplayName("getDailyFeedback_WithoutDate_Success - 날짜 파라미터 없을 때 null을 서비스에 전달")
    void getDailyFeedback_WithoutDate_Success() {
        // given
        LocalDate today = LocalDate.now();
        DailyFeedbackResponse response = createDailyFeedbackResponse(today);

        // Service가 null을 받으면 오늘 날짜로 처리
        given(dailyAnalysisService.getDailyFeedback(principal.userId(), null))
                .willReturn(response);

        // when
        ApiResponse<DailyFeedbackResponse> result =
                weeklyReportController.getDailyFeedback(principal, null);

        // then
        assertThat(result.success()).isTrue();
        assertThat(result.data()).isNotNull();

        // Controller는 null을 그대로 Service에 전달
        then(dailyAnalysisService).should().getDailyFeedback(principal.userId(), null);
    }

    @Test
    @DisplayName("getDailyFeedback_NotFound - 피드백 없을 때 NO_DATA 응답 반환")
    void getDailyFeedback_NotFound() {
        // given
        LocalDate targetDate = LocalDate.of(2024, 1, 15);
        DailyFeedbackResponse noDataResponse = DailyFeedbackResponse.builder()
                .targetDate(targetDate)
                .feedbackText(DefaultReportMessages.DAILY_NO_DATA)
                .dataStatus(ReportDataStatus.NO_DATA)
                .isDefault(true)
                .build();

        given(dailyAnalysisService.getDailyFeedback(principal.userId(), targetDate))
                .willReturn(noDataResponse);

        // when
        ApiResponse<DailyFeedbackResponse> result =
                weeklyReportController.getDailyFeedback(principal, targetDate);

        // then
        assertThat(result.success()).isTrue();
        assertThat(result.data()).isNotNull();
        assertThat(result.data().dataStatus()).isEqualTo(ReportDataStatus.NO_DATA);
        assertThat(result.data().isDefault()).isTrue();
        assertThat(result.data().feedbackText()).isEqualTo(DefaultReportMessages.DAILY_NO_DATA);

        then(dailyAnalysisService).should().getDailyFeedback(principal.userId(), targetDate);
    }

    @Test
    @DisplayName("getDailyFeedback_VerifyServiceCall - 서비스 메서드 호출 검증")
    void getDailyFeedback_VerifyServiceCall() {
        // given
        LocalDate targetDate = LocalDate.of(2024, 1, 20);
        DailyFeedbackResponse response = createDailyFeedbackResponse(targetDate);

        given(dailyAnalysisService.getDailyFeedback(anyLong(), any(LocalDate.class)))
                .willReturn(response);

        // when
        weeklyReportController.getDailyFeedback(principal, targetDate);

        // then
        then(dailyAnalysisService).should().getDailyFeedback(principal.userId(), targetDate);
        then(weeklyReportService).shouldHaveNoInteractions();
        then(monthlyPatternService).shouldHaveNoInteractions();
    }

    private DailyFeedbackResponse createDailyFeedbackResponse(LocalDate targetDate) {
        return DailyFeedbackResponse.builder()
                .targetDate(targetDate)
                .feedbackText("오늘도 열심히 운동하셨네요!")
                .dataStatus(ReportDataStatus.READY)
                .isDefault(false)
                .build();
    }
}
