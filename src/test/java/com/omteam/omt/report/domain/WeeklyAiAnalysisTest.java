package com.omteam.omt.report.domain;

import static org.assertj.core.api.Assertions.*;

import com.omteam.omt.user.domain.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WeeklyAiAnalysisTest {

    @Test
    @DisplayName("Builder로 엔티티 생성 시 모든 필드가 올바르게 설정된다")
    void builder_creates_entity_with_all_fields() {
        // given
        User user = createUser();
        LocalDate weekStartDate = LocalDate.of(2024, 1, 15);
        String mainFailureReason = "시간 부족";
        String overallFeedback = "이번 주는 업무가 많아 운동 시간 확보가 어려웠습니다.";
        LocalDateTime createdAt = LocalDateTime.now();

        // when
        WeeklyAiAnalysis analysis = WeeklyAiAnalysis.builder()
                .id(1L)
                .user(user)
                .weekStartDate(weekStartDate)
                .mainFailureReason(mainFailureReason)
                .overallFeedback(overallFeedback)
                .createdAt(createdAt)
                .build();

        // then
        assertThat(analysis.getId()).isEqualTo(1L);
        assertThat(analysis.getUser()).isEqualTo(user);
        assertThat(analysis.getWeekStartDate()).isEqualTo(weekStartDate);
        assertThat(analysis.getMainFailureReason()).isEqualTo(mainFailureReason);
        assertThat(analysis.getOverallFeedback()).isEqualTo(overallFeedback);
        assertThat(analysis.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("mainFailureReason 필드는 null일 수 있다")
    void mainFailureReason_can_be_null() {
        // given
        User user = createUser();
        LocalDate weekStartDate = LocalDate.of(2024, 1, 15);

        // when
        WeeklyAiAnalysis analysis = WeeklyAiAnalysis.builder()
                .user(user)
                .weekStartDate(weekStartDate)
                .mainFailureReason(null)
                .overallFeedback("피드백")
                .build();

        // then
        assertThat(analysis.getMainFailureReason()).isNull();
    }

    @Test
    @DisplayName("overallFeedback 필드는 null일 수 있다")
    void overallFeedback_can_be_null() {
        // given
        User user = createUser();
        LocalDate weekStartDate = LocalDate.of(2024, 1, 15);

        // when
        WeeklyAiAnalysis analysis = WeeklyAiAnalysis.builder()
                .user(user)
                .weekStartDate(weekStartDate)
                .mainFailureReason("실패 원인")
                .overallFeedback(null)
                .build();

        // then
        assertThat(analysis.getOverallFeedback()).isNull();
    }

    @Test
    @DisplayName("mainFailureReason 최대 길이 200자까지 저장 가능")
    void mainFailureReason_max_length_200() {
        // given
        User user = createUser();
        String longReason = "a".repeat(200);

        // when
        WeeklyAiAnalysis analysis = WeeklyAiAnalysis.builder()
                .user(user)
                .weekStartDate(LocalDate.now())
                .mainFailureReason(longReason)
                .build();

        // then
        assertThat(analysis.getMainFailureReason()).hasSize(200);
    }

    @Test
    @DisplayName("overallFeedback 최대 길이 500자까지 저장 가능")
    void overallFeedback_max_length_500() {
        // given
        User user = createUser();
        String longFeedback = "b".repeat(500);

        // when
        WeeklyAiAnalysis analysis = WeeklyAiAnalysis.builder()
                .user(user)
                .weekStartDate(LocalDate.now())
                .overallFeedback(longFeedback)
                .build();

        // then
        assertThat(analysis.getOverallFeedback()).hasSize(500);
    }

    private User createUser() {
        return User.builder()
                .userId(1L)
                .email("test@example.com")
                .nickname("테스트유저")
                .build();
    }
}
