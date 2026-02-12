package com.omteam.omt.report.dto;

import static org.assertj.core.api.Assertions.*;

import com.omteam.omt.report.domain.DailyAnalysis;
import com.omteam.omt.user.domain.User;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DailyFeedbackResponseTest {

    @Nested
    @DisplayName("DailyFeedbackResponse.from() 메서드 테스트")
    class FromMethodTest {

        @Test
        @DisplayName("DailyAnalysis로부터 DailyFeedbackResponse 생성 성공")
        void from_Success() {
            // given
            DailyAnalysis analysis = DailyAnalysis.builder()
                    .user(createMockUser())
                    .targetDate(LocalDate.of(2024, 1, 15))
                    .feedbackText("오늘의 피드백입니다.")
                    .build();

            // when
            DailyFeedbackResponse response = DailyFeedbackResponse.from(analysis);

            // then
            assertThat(response.targetDate()).isEqualTo(LocalDate.of(2024, 1, 15));
            assertThat(response.feedbackText()).isEqualTo("오늘의 피드백입니다.");
            assertThat(response.dataStatus()).isEqualTo(ReportDataStatus.READY);
            assertThat(response.isDefault()).isFalse();
        }

        @Test
        @DisplayName("feedbackText가 null인 경우에도 정상 변환")
        void from_WithNullFeedbackText() {
            // given
            DailyAnalysis analysis = DailyAnalysis.builder()
                    .user(createMockUser())
                    .targetDate(LocalDate.of(2024, 1, 16))
                    .feedbackText(null)
                    .build();

            // when
            DailyFeedbackResponse response = DailyFeedbackResponse.from(analysis);

            // then
            assertThat(response.targetDate()).isEqualTo(LocalDate.of(2024, 1, 16));
            assertThat(response.feedbackText()).isNull();
            assertThat(response.dataStatus()).isEqualTo(ReportDataStatus.READY);
            assertThat(response.isDefault()).isFalse();
        }
    }

    @Nested
    @DisplayName("DailyFeedbackResponse 생성 테스트")
    class DailyFeedbackResponseCreationTest {

        @Test
        @DisplayName("Builder로 DailyFeedbackResponse 생성 시 모든 필드가 올바르게 설정된다")
        void builder_creates_response_with_all_fields() {
            // given
            LocalDate targetDate = LocalDate.of(2024, 1, 15);

            // when
            DailyFeedbackResponse response = DailyFeedbackResponse.builder()
                    .targetDate(targetDate)
                    .feedbackText("피드백 텍스트")
                    .dataStatus(ReportDataStatus.READY)
                    .isDefault(false)
                    .build();

            // then
            assertThat(response.targetDate()).isEqualTo(targetDate);
            assertThat(response.feedbackText()).isEqualTo("피드백 텍스트");
            assertThat(response.dataStatus()).isEqualTo(ReportDataStatus.READY);
            assertThat(response.isDefault()).isFalse();
        }

        @Test
        @DisplayName("feedbackText가 null인 DailyFeedbackResponse 생성 가능")
        void builder_allows_null_feedbackText() {
            // when
            DailyFeedbackResponse response = DailyFeedbackResponse.builder()
                    .targetDate(LocalDate.of(2024, 1, 15))
                    .feedbackText(null)
                    .dataStatus(ReportDataStatus.NO_DATA)
                    .isDefault(true)
                    .build();

            // then
            assertThat(response.feedbackText()).isNull();
        }
    }

    @Nested
    @DisplayName("Record 불변성 테스트")
    class ImmutabilityTest {

        @Test
        @DisplayName("동일한 값으로 생성된 DailyFeedbackResponse는 equals가 true")
        void records_are_immutable_and_equals() {
            // given
            LocalDate targetDate = LocalDate.of(2024, 1, 15);

            DailyFeedbackResponse response1 = DailyFeedbackResponse.builder()
                    .targetDate(targetDate)
                    .feedbackText("피드백")
                    .dataStatus(ReportDataStatus.READY)
                    .isDefault(false)
                    .build();

            DailyFeedbackResponse response2 = DailyFeedbackResponse.builder()
                    .targetDate(targetDate)
                    .feedbackText("피드백")
                    .dataStatus(ReportDataStatus.READY)
                    .isDefault(false)
                    .build();

            // then
            assertThat(response1).isEqualTo(response2);
            assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
        }
    }

    private User createMockUser() {
        return User.builder()
                .userId(1L)
                .email("test@example.com")
                .nickname("테스트유저")
                .build();
    }
}
