package com.omteam.omt.report.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import com.omteam.omt.common.exception.BusinessException;
import com.omteam.omt.common.exception.ErrorCode;
import com.omteam.omt.common.response.ApiResponse;
import com.omteam.omt.report.dto.DailyFeedbackResponse;
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
        assertThat(result.data().encouragement().title()).isEqualTo("잘하고 계세요!");
        assertThat(result.data().encouragement().message()).isEqualTo("꾸준히 노력하는 모습이 멋집니다.");

        then(dailyAnalysisService).should().getDailyFeedback(principal.userId(), targetDate);
    }

    @Test
    @DisplayName("getDailyFeedback_WithoutDate_Success - 날짜 파라미터 없을 때 오늘 날짜로 조회")
    void getDailyFeedback_WithoutDate_Success() {
        // given
        LocalDate today = LocalDate.now();
        DailyFeedbackResponse response = createDailyFeedbackResponse(today);

        given(dailyAnalysisService.getDailyFeedback(principal.userId(), today))
                .willReturn(response);

        // when
        ApiResponse<DailyFeedbackResponse> result =
                weeklyReportController.getDailyFeedback(principal, null);

        // then
        assertThat(result.success()).isTrue();
        assertThat(result.data()).isNotNull();
        assertThat(result.data().targetDate()).isEqualTo(today);

        then(dailyAnalysisService).should().getDailyFeedback(principal.userId(), today);
    }

    @Test
    @DisplayName("getDailyFeedback_NotFound - 피드백 없을 때 예외 발생")
    void getDailyFeedback_NotFound() {
        // given
        LocalDate targetDate = LocalDate.of(2024, 1, 15);

        given(dailyAnalysisService.getDailyFeedback(principal.userId(), targetDate))
                .willThrow(new BusinessException(ErrorCode.DAILY_FEEDBACK_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> weeklyReportController.getDailyFeedback(principal, targetDate))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DAILY_FEEDBACK_NOT_FOUND);

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
                .encouragement(DailyFeedbackResponse.EncouragementMessageResponse.builder()
                        .title("잘하고 계세요!")
                        .message("꾸준히 노력하는 모습이 멋집니다.")
                        .build())
                .build();
    }
}
