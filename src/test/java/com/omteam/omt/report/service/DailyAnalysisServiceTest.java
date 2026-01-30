package com.omteam.omt.report.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

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

    DailyAnalysisService dailyAnalysisService;

    User testUser;
    LocalDate targetDate;

    @BeforeEach
    void setUp() {
        dailyAnalysisService = new DailyAnalysisService(
                null,
                dailyAnalysisRepository,
                null,
                null
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
        assertThat(response.encouragement().title()).isEqualTo("잘하고 계세요!");
        assertThat(response.encouragement().message()).isEqualTo("꾸준히 노력하는 모습이 멋집니다.");
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
    }

    @Test
    @DisplayName("getDailyFeedback_WithPraiseEncouragement - praise 메시지가 있을 때 우선 선택")
    void getDailyFeedback_WithPraiseEncouragement() {
        // given
        EncouragementMessage praiseMessage = EncouragementMessage.builder()
                .title("칭찬 제목")
                .message("칭찬 메시지")
                .build();

        EncouragementMessage retryMessage = EncouragementMessage.builder()
                .title("재시도 제목")
                .message("재시도 메시지")
                .build();

        EncouragementMessage normalMessage = EncouragementMessage.builder()
                .title("일반 제목")
                .message("일반 메시지")
                .build();

        DailyAnalysis dailyAnalysis = DailyAnalysis.builder()
                .id(1L)
                .user(testUser)
                .feedbackText("피드백 텍스트")
                .targetDate(targetDate)
                .praise(praiseMessage)
                .retry(retryMessage)
                .normal(normalMessage)
                .push(null)
                .build();

        given(dailyAnalysisRepository.findByUserUserIdAndTargetDate(1L, targetDate))
                .willReturn(Optional.of(dailyAnalysis));

        // when
        DailyFeedbackResponse response = dailyAnalysisService.getDailyFeedback(1L, targetDate);

        // then
        assertThat(response.encouragement()).isNotNull();
        assertThat(response.encouragement().title()).isEqualTo("칭찬 제목");
        assertThat(response.encouragement().message()).isEqualTo("칭찬 메시지");
    }

    @Test
    @DisplayName("getDailyFeedback_WithRetryEncouragement - retry 메시지가 있을 때 선택 (praise 없음)")
    void getDailyFeedback_WithRetryEncouragement() {
        // given
        EncouragementMessage retryMessage = EncouragementMessage.builder()
                .title("재시도 제목")
                .message("재시도 메시지")
                .build();

        EncouragementMessage normalMessage = EncouragementMessage.builder()
                .title("일반 제목")
                .message("일반 메시지")
                .build();

        EncouragementMessage pushMessage = EncouragementMessage.builder()
                .title("푸시 제목")
                .message("푸시 메시지")
                .build();

        DailyAnalysis dailyAnalysis = DailyAnalysis.builder()
                .id(1L)
                .user(testUser)
                .feedbackText("피드백 텍스트")
                .targetDate(targetDate)
                .praise(null)
                .retry(retryMessage)
                .normal(normalMessage)
                .push(pushMessage)
                .build();

        given(dailyAnalysisRepository.findByUserUserIdAndTargetDate(1L, targetDate))
                .willReturn(Optional.of(dailyAnalysis));

        // when
        DailyFeedbackResponse response = dailyAnalysisService.getDailyFeedback(1L, targetDate);

        // then
        assertThat(response.encouragement()).isNotNull();
        assertThat(response.encouragement().title()).isEqualTo("재시도 제목");
        assertThat(response.encouragement().message()).isEqualTo("재시도 메시지");
    }

    @Test
    @DisplayName("getDailyFeedback_WithNormalEncouragement - normal 메시지가 있을 때 선택")
    void getDailyFeedback_WithNormalEncouragement() {
        // given
        EncouragementMessage normalMessage = EncouragementMessage.builder()
                .title("일반 제목")
                .message("일반 메시지")
                .build();

        EncouragementMessage pushMessage = EncouragementMessage.builder()
                .title("푸시 제목")
                .message("푸시 메시지")
                .build();

        DailyAnalysis dailyAnalysis = DailyAnalysis.builder()
                .id(1L)
                .user(testUser)
                .feedbackText("피드백 텍스트")
                .targetDate(targetDate)
                .praise(null)
                .retry(null)
                .normal(normalMessage)
                .push(pushMessage)
                .build();

        given(dailyAnalysisRepository.findByUserUserIdAndTargetDate(1L, targetDate))
                .willReturn(Optional.of(dailyAnalysis));

        // when
        DailyFeedbackResponse response = dailyAnalysisService.getDailyFeedback(1L, targetDate);

        // then
        assertThat(response.encouragement()).isNotNull();
        assertThat(response.encouragement().title()).isEqualTo("일반 제목");
        assertThat(response.encouragement().message()).isEqualTo("일반 메시지");
    }

    @Test
    @DisplayName("getDailyFeedback_NoEncouragement - 모든 encouragement가 null일 때")
    void getDailyFeedback_NoEncouragement() {
        // given
        DailyAnalysis dailyAnalysis = DailyAnalysis.builder()
                .id(1L)
                .user(testUser)
                .feedbackText("피드백만 있는 경우")
                .targetDate(targetDate)
                .praise(null)
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
        assertThat(response.feedbackText()).isEqualTo("피드백만 있는 경우");
        assertThat(response.encouragement()).isNull();
    }
}
