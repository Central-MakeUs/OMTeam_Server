package com.omteam.omt.report.dto;

import static org.assertj.core.api.Assertions.*;

import com.omteam.omt.mission.domain.MissionType;
import com.omteam.omt.report.dto.WeeklyReportResponse.AiFeedback;
import com.omteam.omt.report.dto.WeeklyReportResponse.DailyResult;
import com.omteam.omt.report.dto.WeeklyReportResponse.DailyStatus;
import com.omteam.omt.report.dto.WeeklyReportResponse.FailureReasonRank;
import com.omteam.omt.report.dto.WeeklyReportResponse.TypeSuccessCount;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class WeeklyReportResponseTest {

    @Nested
    @DisplayName("WeeklyReportResponse 생성 테스트")
    class WeeklyReportResponseCreationTest {

        @Test
        @DisplayName("Builder로 WeeklyReportResponse 생성 시 모든 필드가 올바르게 설정된다")
        void builder_creates_response_with_all_fields() {
            // given
            LocalDate weekStart = LocalDate.of(2024, 1, 15);
            LocalDate weekEnd = LocalDate.of(2024, 1, 21);

            List<DailyResult> dailyResults = List.of(
                    DailyResult.builder()
                            .date(weekStart)
                            .dayOfWeek(DayOfWeek.MONDAY)
                            .status(DailyStatus.SUCCESS)
                            .missionType(MissionType.EXERCISE)
                            .missionTitle("30분 걷기")
                            .build()
            );

            List<TypeSuccessCount> typeCounts = List.of(
                    TypeSuccessCount.builder()
                            .type(MissionType.EXERCISE)
                            .typeName("운동")
                            .successCount(3)
                            .build()
            );

            List<FailureReasonRank> failureRanks = List.of(
                    FailureReasonRank.builder()
                            .rank(1)
                            .reason("시간 부족")
                            .count(2)
                            .build()
            );

            AiFeedback aiFeedback = AiFeedback.builder()
                    .mainFailureReason("시간 부족")
                    .overallFeedback("이번 주 피드백입니다.")
                    .build();

            // when
            WeeklyReportResponse response = WeeklyReportResponse.builder()
                    .weekStartDate(weekStart)
                    .weekEndDate(weekEnd)
                    .thisWeekSuccessRate(71.4)
                    .lastWeekSuccessRate(57.1)
                    .dailyResults(dailyResults)
                    .typeSuccessCounts(typeCounts)
                    .topFailureReasons(failureRanks)
                    .aiFeedback(aiFeedback)
                    .build();

            // then
            assertThat(response.weekStartDate()).isEqualTo(weekStart);
            assertThat(response.weekEndDate()).isEqualTo(weekEnd);
            assertThat(response.thisWeekSuccessRate()).isEqualTo(71.4);
            assertThat(response.lastWeekSuccessRate()).isEqualTo(57.1);
            assertThat(response.dailyResults()).hasSize(1);
            assertThat(response.typeSuccessCounts()).hasSize(1);
            assertThat(response.topFailureReasons()).hasSize(1);
            assertThat(response.aiFeedback()).isNotNull();
        }

        @Test
        @DisplayName("빈 리스트로 WeeklyReportResponse 생성 가능")
        void builder_allows_empty_lists() {
            // when
            WeeklyReportResponse response = WeeklyReportResponse.builder()
                    .weekStartDate(LocalDate.of(2024, 1, 15))
                    .weekEndDate(LocalDate.of(2024, 1, 21))
                    .thisWeekSuccessRate(0.0)
                    .lastWeekSuccessRate(0.0)
                    .dailyResults(List.of())
                    .typeSuccessCounts(List.of())
                    .topFailureReasons(List.of())
                    .aiFeedback(null)
                    .build();

            // then
            assertThat(response.dailyResults()).isEmpty();
            assertThat(response.typeSuccessCounts()).isEmpty();
            assertThat(response.topFailureReasons()).isEmpty();
            assertThat(response.aiFeedback()).isNull();
        }

        @Test
        @DisplayName("성공률이 0%와 100% 경계값일 때 정상 동작")
        void success_rate_boundary_values() {
            // when
            WeeklyReportResponse zeroRate = WeeklyReportResponse.builder()
                    .weekStartDate(LocalDate.of(2024, 1, 15))
                    .weekEndDate(LocalDate.of(2024, 1, 21))
                    .thisWeekSuccessRate(0.0)
                    .lastWeekSuccessRate(100.0)
                    .dailyResults(List.of())
                    .typeSuccessCounts(List.of())
                    .topFailureReasons(List.of())
                    .aiFeedback(null)
                    .build();

            // then
            assertThat(zeroRate.thisWeekSuccessRate()).isEqualTo(0.0);
            assertThat(zeroRate.lastWeekSuccessRate()).isEqualTo(100.0);
        }
    }

    @Nested
    @DisplayName("DailyResult 테스트")
    class DailyResultTest {

        @Test
        @DisplayName("DailyResult Builder로 생성 성공")
        void builder_creates_daily_result() {
            // given
            LocalDate date = LocalDate.of(2024, 1, 15);

            // when
            DailyResult result = DailyResult.builder()
                    .date(date)
                    .dayOfWeek(DayOfWeek.MONDAY)
                    .status(DailyStatus.SUCCESS)
                    .missionType(MissionType.EXERCISE)
                    .missionTitle("30분 걷기")
                    .build();

            // then
            assertThat(result.date()).isEqualTo(date);
            assertThat(result.dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
            assertThat(result.status()).isEqualTo(DailyStatus.SUCCESS);
            assertThat(result.missionType()).isEqualTo(MissionType.EXERCISE);
            assertThat(result.missionTitle()).isEqualTo("30분 걷기");
        }

        @Test
        @DisplayName("미수행 상태에서 missionType과 missionTitle은 null 가능")
        void not_performed_allows_null_mission_fields() {
            // when
            DailyResult result = DailyResult.builder()
                    .date(LocalDate.of(2024, 1, 15))
                    .dayOfWeek(DayOfWeek.MONDAY)
                    .status(DailyStatus.NOT_PERFORMED)
                    .missionType(null)
                    .missionTitle(null)
                    .build();

            // then
            assertThat(result.status()).isEqualTo(DailyStatus.NOT_PERFORMED);
            assertThat(result.missionType()).isNull();
            assertThat(result.missionTitle()).isNull();
        }

        @Test
        @DisplayName("모든 요일에 대해 DailyResult 생성 가능")
        void all_days_of_week_supported() {
            for (DayOfWeek day : DayOfWeek.values()) {
                DailyResult result = DailyResult.builder()
                        .date(LocalDate.of(2024, 1, 15).plusDays(day.ordinal()))
                        .dayOfWeek(day)
                        .status(DailyStatus.SUCCESS)
                        .missionType(MissionType.EXERCISE)
                        .missionTitle("테스트")
                        .build();

                assertThat(result.dayOfWeek()).isEqualTo(day);
            }
        }
    }

    @Nested
    @DisplayName("DailyStatus 테스트")
    class DailyStatusTest {

        @Test
        @DisplayName("DailyStatus enum 값 확인")
        void daily_status_enum_values() {
            assertThat(DailyStatus.values()).containsExactly(
                    DailyStatus.SUCCESS,
                    DailyStatus.FAILURE,
                    DailyStatus.NOT_PERFORMED
            );
        }

        @Test
        @DisplayName("DailyStatus.valueOf로 enum 조회 가능")
        void daily_status_value_of() {
            assertThat(DailyStatus.valueOf("SUCCESS")).isEqualTo(DailyStatus.SUCCESS);
            assertThat(DailyStatus.valueOf("FAILURE")).isEqualTo(DailyStatus.FAILURE);
            assertThat(DailyStatus.valueOf("NOT_PERFORMED")).isEqualTo(DailyStatus.NOT_PERFORMED);
        }
    }

    @Nested
    @DisplayName("TypeSuccessCount 테스트")
    class TypeSuccessCountTest {

        @Test
        @DisplayName("TypeSuccessCount Builder로 생성 성공")
        void builder_creates_type_success_count() {
            // when
            TypeSuccessCount count = TypeSuccessCount.builder()
                    .type(MissionType.EXERCISE)
                    .typeName("운동")
                    .successCount(5)
                    .build();

            // then
            assertThat(count.type()).isEqualTo(MissionType.EXERCISE);
            assertThat(count.typeName()).isEqualTo("운동");
            assertThat(count.successCount()).isEqualTo(5);
        }

        @Test
        @DisplayName("successCount가 0일 때 정상 동작")
        void zero_success_count_allowed() {
            // when
            TypeSuccessCount count = TypeSuccessCount.builder()
                    .type(MissionType.DIET)
                    .typeName("식단")
                    .successCount(0)
                    .build();

            // then
            assertThat(count.successCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("EXERCISE와 DIET 타입 모두 지원")
        void both_mission_types_supported() {
            TypeSuccessCount exercise = TypeSuccessCount.builder()
                    .type(MissionType.EXERCISE)
                    .typeName("운동")
                    .successCount(3)
                    .build();

            TypeSuccessCount diet = TypeSuccessCount.builder()
                    .type(MissionType.DIET)
                    .typeName("식단")
                    .successCount(2)
                    .build();

            assertThat(exercise.type()).isEqualTo(MissionType.EXERCISE);
            assertThat(diet.type()).isEqualTo(MissionType.DIET);
        }
    }

    @Nested
    @DisplayName("FailureReasonRank 테스트")
    class FailureReasonRankTest {

        @Test
        @DisplayName("FailureReasonRank Builder로 생성 성공")
        void builder_creates_failure_reason_rank() {
            // when
            FailureReasonRank rank = FailureReasonRank.builder()
                    .rank(1)
                    .reason("시간 부족")
                    .count(5)
                    .build();

            // then
            assertThat(rank.rank()).isEqualTo(1);
            assertThat(rank.reason()).isEqualTo("시간 부족");
            assertThat(rank.count()).isEqualTo(5);
        }

        @Test
        @DisplayName("순위 1, 2, 3위 생성 테스트")
        void create_top_3_ranks() {
            List<FailureReasonRank> ranks = List.of(
                    FailureReasonRank.builder().rank(1).reason("시간 부족").count(5).build(),
                    FailureReasonRank.builder().rank(2).reason("피로").count(3).build(),
                    FailureReasonRank.builder().rank(3).reason("날씨").count(1).build()
            );

            assertThat(ranks).hasSize(3);
            assertThat(ranks.get(0).rank()).isEqualTo(1);
            assertThat(ranks.get(1).rank()).isEqualTo(2);
            assertThat(ranks.get(2).rank()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("AiFeedback 테스트")
    class AiFeedbackTest {

        @Test
        @DisplayName("AiFeedback Builder로 생성 성공")
        void builder_creates_ai_feedback() {
            // when
            AiFeedback feedback = AiFeedback.builder()
                    .mainFailureReason("시간 부족")
                    .overallFeedback("이번 주는 업무가 많아 운동 시간 확보가 어려웠습니다.")
                    .build();

            // then
            assertThat(feedback.mainFailureReason()).isEqualTo("시간 부족");
            assertThat(feedback.overallFeedback()).isEqualTo("이번 주는 업무가 많아 운동 시간 확보가 어려웠습니다.");
        }

        @Test
        @DisplayName("AI 피드백이 없을 때 null 허용")
        void null_feedback_allowed() {
            // when
            AiFeedback feedback = AiFeedback.builder()
                    .mainFailureReason(null)
                    .overallFeedback(null)
                    .build();

            // then
            assertThat(feedback.mainFailureReason()).isNull();
            assertThat(feedback.overallFeedback()).isNull();
        }

        @Test
        @DisplayName("긴 피드백 텍스트 저장 가능")
        void long_feedback_text_allowed() {
            // given
            String longFeedback = "이번 주는 " + "정말 ".repeat(50) + "힘들었습니다.";

            // when
            AiFeedback feedback = AiFeedback.builder()
                    .mainFailureReason("복합적인 원인")
                    .overallFeedback(longFeedback)
                    .build();

            // then
            assertThat(feedback.overallFeedback()).isEqualTo(longFeedback);
        }
    }

    @Nested
    @DisplayName("Record 불변성 테스트")
    class ImmutabilityTest {

        @Test
        @DisplayName("Record는 불변이므로 동일한 값으로 생성된 객체는 equals가 true")
        void records_are_immutable_and_equals() {
            // given
            AiFeedback feedback1 = AiFeedback.builder()
                    .mainFailureReason("시간 부족")
                    .overallFeedback("피드백")
                    .build();

            AiFeedback feedback2 = AiFeedback.builder()
                    .mainFailureReason("시간 부족")
                    .overallFeedback("피드백")
                    .build();

            // then
            assertThat(feedback1).isEqualTo(feedback2);
            assertThat(feedback1.hashCode()).isEqualTo(feedback2.hashCode());
        }
    }
}
