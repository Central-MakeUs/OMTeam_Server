package com.omteam.omt.report.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import com.omteam.omt.common.ai.dto.UserContext;
import com.omteam.omt.common.ai.service.UserContextService;
import com.omteam.omt.common.exception.BusinessException;
import com.omteam.omt.common.exception.ErrorCode;
import com.omteam.omt.report.domain.DailyAnalysis;
import com.omteam.omt.report.domain.EncouragementMessage;
import com.omteam.omt.report.dto.DailyFeedbackResponse;
import com.omteam.omt.report.repository.DailyAnalysisRepository;
import com.omteam.omt.user.domain.User;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DailyAnalysisServiceTest {

    @Mock
    DailyAnalysisRepository dailyAnalysisRepository;

    @Mock
    UserContextService userContextService;

    DailyAnalysisService dailyAnalysisService;

    User testUser;
    LocalDate targetDate;

    @BeforeEach
    void setUp() {
        dailyAnalysisService = new DailyAnalysisService(
                null,
                dailyAnalysisRepository,
                null,
                null,
                userContextService
        );

        testUser = User.builder()
                .userId(1L)
                .email("test@example.com")
                .nickname("테스트유저")
                .build();

        targetDate = LocalDate.of(2024, 1, 15);
    }

    @Test
    @DisplayName("getDailyFeedback_Success - 정상 조회 성공")
    void getDailyFeedback_Success() {
        // given
        EncouragementMessage praiseMessage = EncouragementMessage.builder()
                .title("잘하고 계세요!")
                .message("꾸준히 노력하는 모습이 멋집니다.")
                .build();

        DailyAnalysis dailyAnalysis = DailyAnalysis.builder()
                .id(1L)
                .user(testUser)
                .feedbackText("오늘도 열심히 운동하셨네요!")
                .targetDate(targetDate)
                .praise(praiseMessage)
                .retry(null)
                .normal(null)
                .push(null)
                .build();

        given(dailyAnalysisRepository.findByUserUserIdAndTargetDate(1L, targetDate))
                .willReturn(Optional.of(dailyAnalysis));

        // when
        DailyFeedbackResponse response = dailyAnalysisService.getDailyFeedback(1L, targetDate);

        // then
        assertThat(response).isNotNull();
        assertThat(response.targetDate()).isEqualTo(targetDate);
        assertThat(response.feedbackText()).isEqualTo("오늘도 열심히 운동하셨네요!");
        assertThat(response.encouragement()).isNotNull();

        then(dailyAnalysisRepository).should().findByUserUserIdAndTargetDate(1L, targetDate);
    }

    @Test
    @DisplayName("getDailyFeedback_NotFound - 피드백 없음")
    void getDailyFeedback_NotFound() {
        // given
        given(dailyAnalysisRepository.findByUserUserIdAndTargetDate(1L, targetDate))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> dailyAnalysisService.getDailyFeedback(1L, targetDate))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DAILY_FEEDBACK_NOT_FOUND);

        then(dailyAnalysisRepository).should().findByUserUserIdAndTargetDate(1L, targetDate);
    }

    @Test
    @DisplayName("getDailyFeedback_WithNullDate - 날짜가 null이면 오늘 날짜로 조회")
    void getDailyFeedback_WithNullDate() {
        // given
        LocalDate today = LocalDate.now();
        DailyAnalysis dailyAnalysis = DailyAnalysis.builder()
                .id(1L)
                .user(testUser)
                .feedbackText("오늘의 피드백")
                .targetDate(today)
                .praise(null)
                .retry(null)
                .normal(null)
                .push(null)
                .build();

        given(dailyAnalysisRepository.findByUserUserIdAndTargetDate(1L, today))
                .willReturn(Optional.of(dailyAnalysis));

        // when
        DailyFeedbackResponse response = dailyAnalysisService.getDailyFeedback(1L, null);

        // then
        assertThat(response).isNotNull();
        assertThat(response.targetDate()).isEqualTo(today);

        then(dailyAnalysisRepository).should().findByUserUserIdAndTargetDate(1L, today);
    }
}
