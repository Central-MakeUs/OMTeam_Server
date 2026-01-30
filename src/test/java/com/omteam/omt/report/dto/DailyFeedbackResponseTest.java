package com.omteam.omt.report.dto;

import static org.assertj.core.api.Assertions.*;

import com.omteam.omt.report.domain.DailyAnalysis;
import com.omteam.omt.report.domain.EncouragementMessage;
import com.omteam.omt.report.dto.DailyFeedbackResponse.EncouragementMessageResponse;
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
        @DisplayName("praise 메시지가 있을 때 우선 선택된다")
        void from_WithPraiseEncouragement() {
            // given
            EncouragementMessage praise = EncouragementMessage.builder()
                    .title("잘하고 계세요!")
                    .message("오늘도 훌륭한 하루였습니다.")
                    .build();

            EncouragementMessage retry = EncouragementMessage.builder()
                    .title("다시 시도해보세요")
                    .message("retry 메시지")
                    .build();

            DailyAnalysis analysis = DailyAnalysis.builder()
                    .user(createMockUser())
                    .targetDate(LocalDate.of(2024, 1, 15))
                    .feedbackText("오늘의 피드백입니다.")
                    .praise(praise)
                    .retry(retry)
                    .normal(null)
                    .push(null)
                    .build();

            // when
            DailyFeedbackResponse response = DailyFeedbackResponse.from(analysis);

            // then
            assertThat(response.targetDate()).isEqualTo(LocalDate.of(2024, 1, 15));
            assertThat(response.feedbackText()).isEqualTo("오늘의 피드백입니다.");
            assertThat(response.encouragement()).isNotNull();
            assertThat(response.encouragement().title()).isEqualTo("잘하고 계세요!");
            assertThat(response.encouragement().message()).isEqualTo("오늘도 훌륭한 하루였습니다.");
        }

        @Test
        @DisplayName("praise 없고 retry만 있을 때 retry가 선택된다")
        void from_WithRetryEncouragementOnly() {
            // given
            EncouragementMessage retry = EncouragementMessage.builder()
                    .title("다시 시도해보세요")
                    .message("내일은 더 잘할 수 있어요.")
                    .build();

            DailyAnalysis analysis = DailyAnalysis.builder()
                    .user(createMockUser())
                    .targetDate(LocalDate.of(2024, 1, 16))
                    .feedbackText("다시 한번 해봅시다.")
                    .praise(null)
                    .retry(retry)
                    .normal(null)
                    .push(null)
                    .build();

            // when
            DailyFeedbackResponse response = DailyFeedbackResponse.from(analysis);

            // then
            assertThat(response.encouragement()).isNotNull();
            assertThat(response.encouragement().title()).isEqualTo("다시 시도해보세요");
            assertThat(response.encouragement().message()).isEqualTo("내일은 더 잘할 수 있어요.");
        }

        @Test
        @DisplayName("praise, retry 없고 normal만 있을 때 normal이 선택된다")
        void from_WithNormalEncouragementOnly() {
            // given
            EncouragementMessage normal = EncouragementMessage.builder()
                    .title("평범한 하루")
                    .message("꾸준함이 중요해요.")
                    .build();

            DailyAnalysis analysis = DailyAnalysis.builder()
                    .user(createMockUser())
                    .targetDate(LocalDate.of(2024, 1, 17))
                    .feedbackText("일상적인 피드백")
                    .praise(null)
                    .retry(null)
                    .normal(normal)
                    .push(null)
                    .build();

            // when
            DailyFeedbackResponse response = DailyFeedbackResponse.from(analysis);

            // then
            assertThat(response.encouragement()).isNotNull();
            assertThat(response.encouragement().title()).isEqualTo("평범한 하루");
            assertThat(response.encouragement().message()).isEqualTo("꾸준함이 중요해요.");
        }

        @Test
        @DisplayName("push만 있을 때 push가 선택된다")
        void from_WithPushEncouragementOnly() {
            // given
            EncouragementMessage push = EncouragementMessage.builder()
                    .title("힘내세요!")
                    .message("조금만 더 힘을 내봐요.")
                    .build();

            DailyAnalysis analysis = DailyAnalysis.builder()
                    .user(createMockUser())
                    .targetDate(LocalDate.of(2024, 1, 18))
                    .feedbackText("응원 메시지")
                    .praise(null)
                    .retry(null)
                    .normal(null)
                    .push(push)
                    .build();

            // when
            DailyFeedbackResponse response = DailyFeedbackResponse.from(analysis);

            // then
            assertThat(response.encouragement()).isNotNull();
            assertThat(response.encouragement().title()).isEqualTo("힘내세요!");
            assertThat(response.encouragement().message()).isEqualTo("조금만 더 힘을 내봐요.");
        }

        @Test
        @DisplayName("모든 메시지가 있을 때 praise가 우선 선택된다")
        void from_WithAllEncouragements() {
            // given
            EncouragementMessage praise = EncouragementMessage.builder()
                    .title("praise 타이틀")
                    .message("praise 메시지")
                    .build();

            EncouragementMessage retry = EncouragementMessage.builder()
                    .title("retry 타이틀")
                    .message("retry 메시지")
                    .build();

            EncouragementMessage normal = EncouragementMessage.builder()
                    .title("normal 타이틀")
                    .message("normal 메시지")
                    .build();

            EncouragementMessage push = EncouragementMessage.builder()
                    .title("push 타이틀")
                    .message("push 메시지")
                    .build();

            DailyAnalysis analysis = DailyAnalysis.builder()
                    .user(createMockUser())
                    .targetDate(LocalDate.of(2024, 1, 19))
                    .feedbackText("모든 메시지 테스트")
                    .praise(praise)
                    .retry(retry)
                    .normal(normal)
                    .push(push)
                    .build();

            // when
            DailyFeedbackResponse response = DailyFeedbackResponse.from(analysis);

            // then
            assertThat(response.encouragement()).isNotNull();
            assertThat(response.encouragement().title()).isEqualTo("praise 타이틀");
            assertThat(response.encouragement().message()).isEqualTo("praise 메시지");
        }

        @Test
        @DisplayName("모든 encouragement가 null일 때 encouragement는 null로 반환된다")
        void from_WithNoEncouragement() {
            // given
            DailyAnalysis analysis = DailyAnalysis.builder()
                    .user(createMockUser())
                    .targetDate(LocalDate.of(2024, 1, 20))
                    .feedbackText("메시지 없는 피드백")
                    .praise(null)
                    .retry(null)
                    .normal(null)
                    .push(null)
                    .build();

            // when
            DailyFeedbackResponse response = DailyFeedbackResponse.from(analysis);

            // then
            assertThat(response.targetDate()).isEqualTo(LocalDate.of(2024, 1, 20));
            assertThat(response.feedbackText()).isEqualTo("메시지 없는 피드백");
            assertThat(response.encouragement()).isNull();
        }

        @Test
        @DisplayName("우선순위 검증: praise > retry > normal > push")
        void from_PriorityValidation() {
            // given - praise 없음, retry부터 시작
            DailyAnalysis analysisWithRetry = DailyAnalysis.builder()
                    .user(createMockUser())
                    .targetDate(LocalDate.of(2024, 1, 21))
                    .feedbackText("테스트")
                    .praise(null)
                    .retry(EncouragementMessage.builder().title("retry").message("retry").build())
                    .normal(EncouragementMessage.builder().title("normal").message("normal").build())
                    .push(EncouragementMessage.builder().title("push").message("push").build())
                    .build();

            // when
            DailyFeedbackResponse response1 = DailyFeedbackResponse.from(analysisWithRetry);

            // then - retry 선택됨
            assertThat(response1.encouragement().title()).isEqualTo("retry");

            // given - retry 없음, normal부터 시작
            DailyAnalysis analysisWithNormal = DailyAnalysis.builder()
                    .user(createMockUser())
                    .targetDate(LocalDate.of(2024, 1, 22))
                    .feedbackText("테스트")
                    .praise(null)
                    .retry(null)
                    .normal(EncouragementMessage.builder().title("normal").message("normal").build())
                    .push(EncouragementMessage.builder().title("push").message("push").build())
                    .build();

            // when
            DailyFeedbackResponse response2 = DailyFeedbackResponse.from(analysisWithNormal);

            // then - normal 선택됨
            assertThat(response2.encouragement().title()).isEqualTo("normal");
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
            EncouragementMessageResponse encouragement = EncouragementMessageResponse.builder()
                    .title("제목")
                    .message("메시지")
                    .build();

            // when
            DailyFeedbackResponse response = DailyFeedbackResponse.builder()
                    .targetDate(targetDate)
                    .feedbackText("피드백 텍스트")
                    .encouragement(encouragement)
                    .build();

            // then
            assertThat(response.targetDate()).isEqualTo(targetDate);
            assertThat(response.feedbackText()).isEqualTo("피드백 텍스트");
            assertThat(response.encouragement()).isNotNull();
            assertThat(response.encouragement().title()).isEqualTo("제목");
            assertThat(response.encouragement().message()).isEqualTo("메시지");
        }

        @Test
        @DisplayName("encouragement가 null인 DailyFeedbackResponse 생성 가능")
        void builder_allows_null_encouragement() {
            // when
            DailyFeedbackResponse response = DailyFeedbackResponse.builder()
                    .targetDate(LocalDate.of(2024, 1, 15))
                    .feedbackText("피드백만 있음")
                    .encouragement(null)
                    .build();

            // then
            assertThat(response.encouragement()).isNull();
        }
    }

    @Nested
    @DisplayName("EncouragementMessageResponse 테스트")
    class EncouragementMessageResponseTest {

        @Test
        @DisplayName("Builder로 EncouragementMessageResponse 생성 성공")
        void builder_creates_encouragement_message_response() {
            // when
            EncouragementMessageResponse response = EncouragementMessageResponse.builder()
                    .title("잘하고 있어요")
                    .message("계속 노력하세요")
                    .build();

            // then
            assertThat(response.title()).isEqualTo("잘하고 있어요");
            assertThat(response.message()).isEqualTo("계속 노력하세요");
        }

        @Test
        @DisplayName("긴 텍스트도 정상 저장된다")
        void long_text_allowed() {
            // given
            String longTitle = "매우 " + "긴 ".repeat(20) + "제목";
            String longMessage = "매우 " + "긴 ".repeat(100) + "메시지";

            // when
            EncouragementMessageResponse response = EncouragementMessageResponse.builder()
                    .title(longTitle)
                    .message(longMessage)
                    .build();

            // then
            assertThat(response.title()).isEqualTo(longTitle);
            assertThat(response.message()).isEqualTo(longMessage);
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
            EncouragementMessageResponse encouragement = EncouragementMessageResponse.builder()
                    .title("제목")
                    .message("메시지")
                    .build();

            DailyFeedbackResponse response1 = DailyFeedbackResponse.builder()
                    .targetDate(targetDate)
                    .feedbackText("피드백")
                    .encouragement(encouragement)
                    .build();

            DailyFeedbackResponse response2 = DailyFeedbackResponse.builder()
                    .targetDate(targetDate)
                    .feedbackText("피드백")
                    .encouragement(encouragement)
                    .build();

            // then
            assertThat(response1).isEqualTo(response2);
            assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
        }

        @Test
        @DisplayName("동일한 값으로 생성된 EncouragementMessageResponse는 equals가 true")
        void encouragement_message_records_are_immutable_and_equals() {
            // given
            EncouragementMessageResponse message1 = EncouragementMessageResponse.builder()
                    .title("제목")
                    .message("메시지")
                    .build();

            EncouragementMessageResponse message2 = EncouragementMessageResponse.builder()
                    .title("제목")
                    .message("메시지")
                    .build();

            // then
            assertThat(message1).isEqualTo(message2);
            assertThat(message1.hashCode()).isEqualTo(message2.hashCode());
        }
    }

    /**
     * 테스트용 Mock User 생성
     */
    private User createMockUser() {
        return User.builder()
                .userId(1L)
                .email("test@example.com")
                .nickname("테스트유저")
                .build();
    }
}
