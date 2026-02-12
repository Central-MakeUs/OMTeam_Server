package com.omteam.omt.report.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omteam.omt.common.exception.BusinessException;
import com.omteam.omt.common.exception.ErrorCode;
import com.omteam.omt.mission.domain.DailyMissionResult;
import com.omteam.omt.mission.domain.Mission;
import com.omteam.omt.mission.domain.MissionResult;
import com.omteam.omt.mission.domain.MissionType;
import com.omteam.omt.mission.repository.DailyMissionResultRepository;
import com.omteam.omt.report.client.dto.AiWeeklyAnalysisResponse;
import com.omteam.omt.report.constant.DefaultReportMessages;
import com.omteam.omt.report.domain.WeeklyAiAnalysis;
import com.omteam.omt.report.dto.ReportDataStatus;
import com.omteam.omt.report.dto.WeeklyReportResponse;
import com.omteam.omt.report.dto.WeeklyReportResponse.DailyStatus;
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
class WeeklyReportServiceTest {

    @Mock
    DailyMissionResultRepository missionResultRepository;

    @Mock
    WeeklyAiAnalysisRepository weeklyAiAnalysisRepository;

    @Mock
    ObjectMapper objectMapper;

    WeeklyReportService weeklyReportService;

    final Long userId = 1L;
    final LocalDate monday = LocalDate.of(2024, 1, 15); // Monday
    // 2024년 1월 15일은 1월의 3번째 월요일 (첫 번째 월요일: 1/1)
    final Integer year = 2024;
    final Integer month = 1;
    final Integer weekOfMonth = 3;

    @BeforeEach
    void setUp() {
        weeklyReportService = new WeeklyReportService(
                missionResultRepository,
                weeklyAiAnalysisRepository,
                objectMapper
        );
    }

    @Nested
    @DisplayName("성공률 계산 테스트")
    class SuccessRateTest {

        @Test
        @DisplayName("이번 주 성공률 계산 - 7일 중 5일 성공")
        void calculateThisWeekSuccessRate() {
            // given
            List<DailyMissionResult> results = List.of(
                    createMissionResult(monday, MissionResult.SUCCESS),
                    createMissionResult(monday.plusDays(1), MissionResult.SUCCESS),
                    createMissionResult(monday.plusDays(2), MissionResult.SUCCESS),
                    createMissionResult(monday.plusDays(3), MissionResult.FAILURE),
                    createMissionResult(monday.plusDays(4), MissionResult.SUCCESS),
                    createMissionResult(monday.plusDays(5), MissionResult.FAILURE),
                    createMissionResult(monday.plusDays(6), MissionResult.SUCCESS)
            );

            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(
                    eq(userId), eq(monday), eq(monday.plusDays(6))))
                    .willReturn(results);
            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(
                    eq(userId), eq(monday.minusDays(7)), eq(monday.minusDays(1))))
                    .willReturn(List.of());
            given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(userId, monday))
                    .willReturn(Optional.empty());

            // when
            WeeklyReportResponse response = weeklyReportService.getWeeklyReport(userId, year, month, weekOfMonth);

            // then
            assertThat(response.dataStatus()).isEqualTo(ReportDataStatus.PENDING_ANALYSIS);
            assertThat(response.thisWeekSuccessRate()).isEqualTo(71.4); // 5/7 = 71.4%
            assertThat(response.thisWeekSuccessCount()).isEqualTo(5);
        }

        @Test
        @DisplayName("지난 주 성공률 계산")
        void calculateLastWeekSuccessRate() {
            // given
            LocalDate lastWeekMonday = monday.minusDays(7);
            List<DailyMissionResult> lastWeekResults = List.of(
                    createMissionResult(lastWeekMonday, MissionResult.SUCCESS),
                    createMissionResult(lastWeekMonday.plusDays(1), MissionResult.SUCCESS),
                    createMissionResult(lastWeekMonday.plusDays(2), MissionResult.SUCCESS),
                    createMissionResult(lastWeekMonday.plusDays(3), MissionResult.SUCCESS),
                    createMissionResult(lastWeekMonday.plusDays(4), MissionResult.FAILURE),
                    createMissionResult(lastWeekMonday.plusDays(5), MissionResult.FAILURE),
                    createMissionResult(lastWeekMonday.plusDays(6), MissionResult.FAILURE)
            );

            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(
                    eq(userId), eq(monday), eq(monday.plusDays(6))))
                    .willReturn(List.of());
            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(
                    eq(userId), eq(lastWeekMonday), eq(monday.minusDays(1))))
                    .willReturn(lastWeekResults);
            given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(userId, monday))
                    .willReturn(Optional.empty());

            // when
            WeeklyReportResponse response = weeklyReportService.getWeeklyReport(userId, year, month, weekOfMonth);

            // then
            assertThat(response.dataStatus()).isEqualTo(ReportDataStatus.NO_DATA);
            assertThat(response.lastWeekSuccessRate()).isEqualTo(57.1); // 4/7 = 57.1%
        }

        @Test
        @DisplayName("결과가 없을 때 성공률 0%")
        void zeroSuccessRateWhenNoResults() {
            // given
            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(
                    anyLong(), any(), any()))
                    .willReturn(List.of());
            given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(anyLong(), any()))
                    .willReturn(Optional.empty());

            // when
            WeeklyReportResponse response = weeklyReportService.getWeeklyReport(userId, year, month, weekOfMonth);

            // then
            assertThat(response.dataStatus()).isEqualTo(ReportDataStatus.NO_DATA);
            assertThat(response.thisWeekSuccessRate()).isEqualTo(0.0);
            assertThat(response.lastWeekSuccessRate()).isEqualTo(0.0);
            assertThat(response.thisWeekSuccessCount()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("요일별 결과 조회 테스트")
    class DailyResultsTest {

        @Test
        @DisplayName("요일별 결과 목록 생성 - 성공, 실패, 미수행 포함")
        void buildDailyResults() {
            // given
            List<DailyMissionResult> results = List.of(
                    createMissionResult(monday, MissionResult.SUCCESS),
                    createMissionResult(monday.plusDays(2), MissionResult.FAILURE)
            );

            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(
                    eq(userId), eq(monday), eq(monday.plusDays(6))))
                    .willReturn(results);
            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(
                    eq(userId), eq(monday.minusDays(7)), eq(monday.minusDays(1))))
                    .willReturn(List.of());
            given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(userId, monday))
                    .willReturn(Optional.empty());

            // when
            WeeklyReportResponse response = weeklyReportService.getWeeklyReport(userId, year, month, weekOfMonth);

            // then
            assertThat(response.dataStatus()).isEqualTo(ReportDataStatus.PENDING_ANALYSIS);
            assertThat(response.dailyResults()).isNotEmpty();
            assertThat(response.dailyResults().get(0).status()).isEqualTo(DailyStatus.SUCCESS);
            assertThat(response.dailyResults().get(0).dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        }
    }

    @Nested
    @DisplayName("타입별 성공횟수 테스트")
    class TypeSuccessCountTest {

        @Test
        @DisplayName("운동 타입 3회 성공, 식단 타입 2회 성공")
        void calculateTypeSuccessCounts() {
            // given
            List<DailyMissionResult> results = List.of(
                    createMissionResult(monday, MissionResult.SUCCESS, MissionType.EXERCISE),
                    createMissionResult(monday.plusDays(1), MissionResult.SUCCESS, MissionType.EXERCISE),
                    createMissionResult(monday.plusDays(2), MissionResult.SUCCESS, MissionType.EXERCISE),
                    createMissionResult(monday.plusDays(3), MissionResult.SUCCESS, MissionType.DIET),
                    createMissionResult(monday.plusDays(4), MissionResult.SUCCESS, MissionType.DIET),
                    createMissionResult(monday.plusDays(5), MissionResult.FAILURE, MissionType.EXERCISE)
            );

            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(
                    eq(userId), eq(monday), eq(monday.plusDays(6))))
                    .willReturn(results);
            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(
                    eq(userId), eq(monday.minusDays(7)), eq(monday.minusDays(1))))
                    .willReturn(List.of());
            given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(userId, monday))
                    .willReturn(Optional.empty());

            // when
            WeeklyReportResponse response = weeklyReportService.getWeeklyReport(userId, year, month, weekOfMonth);

            // then
            assertThat(response.dataStatus()).isEqualTo(ReportDataStatus.PENDING_ANALYSIS);
            assertThat(response.typeSuccessCounts()).hasSize(2);
            assertThat(response.typeSuccessCounts().stream()
                    .filter(t -> t.type() == MissionType.EXERCISE)
                    .findFirst().get().successCount()).isEqualTo(3);
            assertThat(response.typeSuccessCounts().stream()
                    .filter(t -> t.type() == MissionType.DIET)
                    .findFirst().get().successCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("모든 MissionType이 포함되며 각 타입의 displayName이 올바르게 설정됨")
        void allMissionTypesIncluded() {
            // given
            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(
                    eq(userId), eq(monday), eq(monday.plusDays(6))))
                    .willReturn(List.of());
            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(
                    eq(userId), eq(monday.minusDays(7)), eq(monday.minusDays(1))))
                    .willReturn(List.of());
            given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(userId, monday))
                    .willReturn(Optional.empty());

            // when
            WeeklyReportResponse response = weeklyReportService.getWeeklyReport(userId, year, month, weekOfMonth);

            // then
            assertThat(response.dataStatus()).isEqualTo(ReportDataStatus.NO_DATA);
            assertThat(response.typeSuccessCounts()).hasSize(MissionType.values().length);
            for (MissionType type : MissionType.values()) {
                assertThat(response.typeSuccessCounts()).anySatisfy(count -> {
                    assertThat(count.type()).isEqualTo(type);
                    assertThat(count.typeName()).isEqualTo(type.getDisplayName());
                });
            }
        }

        @Test
        @DisplayName("특정 타입만 성공 기록이 있어도 모든 타입이 반환됨")
        void allTypesReturnedEvenWhenOnlyOneTypeHasSuccess() {
            // given
            List<DailyMissionResult> results = List.of(
                    createMissionResult(monday, MissionResult.SUCCESS, MissionType.EXERCISE),
                    createMissionResult(monday.plusDays(1), MissionResult.SUCCESS, MissionType.EXERCISE)
            );

            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(
                    eq(userId), eq(monday), eq(monday.plusDays(6))))
                    .willReturn(results);
            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(
                    eq(userId), eq(monday.minusDays(7)), eq(monday.minusDays(1))))
                    .willReturn(List.of());
            given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(userId, monday))
                    .willReturn(Optional.empty());

            // when
            WeeklyReportResponse response = weeklyReportService.getWeeklyReport(userId, year, month, weekOfMonth);

            // then
            assertThat(response.dataStatus()).isEqualTo(ReportDataStatus.PENDING_ANALYSIS);
            assertThat(response.typeSuccessCounts()).hasSize(MissionType.values().length);
            assertThat(response.typeSuccessCounts().stream()
                    .filter(t -> t.type() == MissionType.EXERCISE)
                    .findFirst().get().successCount()).isEqualTo(2);
            assertThat(response.typeSuccessCounts().stream()
                    .filter(t -> t.type() == MissionType.DIET)
                    .findFirst().get().successCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("실패한 미션은 성공 횟수에 포함되지 않음")
        void failedMissionsNotCountedInSuccessCount() {
            // given
            List<DailyMissionResult> results = List.of(
                    createMissionResult(monday, MissionResult.SUCCESS, MissionType.EXERCISE),
                    createMissionResult(monday.plusDays(1), MissionResult.FAILURE, MissionType.EXERCISE),
                    createMissionResult(monday.plusDays(2), MissionResult.FAILURE, MissionType.DIET),
                    createMissionResult(monday.plusDays(3), MissionResult.SUCCESS, MissionType.DIET)
            );

            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(
                    eq(userId), eq(monday), eq(monday.plusDays(6))))
                    .willReturn(results);
            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(
                    eq(userId), eq(monday.minusDays(7)), eq(monday.minusDays(1))))
                    .willReturn(List.of());
            given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(userId, monday))
                    .willReturn(Optional.empty());

            // when
            WeeklyReportResponse response = weeklyReportService.getWeeklyReport(userId, year, month, weekOfMonth);

            // then
            assertThat(response.dataStatus()).isEqualTo(ReportDataStatus.PENDING_ANALYSIS);
            assertThat(response.typeSuccessCounts())
                    .anySatisfy(count -> {
                        assertThat(count.type()).isEqualTo(MissionType.EXERCISE);
                        assertThat(count.successCount()).isEqualTo(1);
                    })
                    .anySatisfy(count -> {
                        assertThat(count.type()).isEqualTo(MissionType.DIET);
                        assertThat(count.successCount()).isEqualTo(1);
                    });
        }
    }

    @Nested
    @DisplayName("실패 원인 순위 테스트")
    class FailureReasonRankTest {

        @Test
        @DisplayName("AI 분석 결과에서 실패 원인 순위를 추출하여 반환")
        void getTopFailureReasonsFromAiAnalysis() throws Exception {
            // given
            User user = User.builder().userId(userId).build();
            String rankingJson = "[{\"rank\":1,\"category\":\"시간 부족\",\"count\":3}," +
                    "{\"rank\":2,\"category\":\"피로\",\"count\":2}," +
                    "{\"rank\":3,\"category\":\"날씨\",\"count\":1}]";

            WeeklyAiAnalysis analysis = WeeklyAiAnalysis.builder()
                    .user(user)
                    .weekStartDate(monday)
                    .failureReasonRankingJson(rankingJson)
                    .weeklyFeedback("이번주 피드백")
                    .build();

            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(
                    anyLong(), any(), any()))
                    .willReturn(List.of());
            given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(userId, monday))
                    .willReturn(Optional.of(analysis));
            given(objectMapper.readValue(eq(rankingJson), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                    .willReturn(List.of(
                            createFailureReasonRank(1, "시간 부족", 3),
                            createFailureReasonRank(2, "피로", 2),
                            createFailureReasonRank(3, "날씨", 1)
                    ));

            // when
            WeeklyReportResponse response = weeklyReportService.getWeeklyReport(userId, year, month, weekOfMonth);

            // then
            assertThat(response.dataStatus()).isEqualTo(ReportDataStatus.READY);
            assertThat(response.topFailureReasons()).hasSize(3);
            assertThat(response.topFailureReasons().get(0).rank()).isEqualTo(1);
            assertThat(response.topFailureReasons().get(0).reason()).isEqualTo("시간 부족");
            assertThat(response.topFailureReasons().get(0).count()).isEqualTo(3);
            assertThat(response.topFailureReasons().get(1).rank()).isEqualTo(2);
            assertThat(response.topFailureReasons().get(1).reason()).isEqualTo("피로");
            assertThat(response.topFailureReasons().get(2).rank()).isEqualTo(3);
            assertThat(response.topFailureReasons().get(2).reason()).isEqualTo("날씨");
        }

        @Test
        @DisplayName("AI 분석 결과가 없으면 빈 리스트 반환")
        void emptyFailureReasonsWhenNoAiAnalysis() {
            // given
            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(
                    anyLong(), any(), any()))
                    .willReturn(List.of());
            given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(userId, monday))
                    .willReturn(Optional.empty());

            // when
            WeeklyReportResponse response = weeklyReportService.getWeeklyReport(userId, year, month, weekOfMonth);

            // then
            assertThat(response.dataStatus()).isEqualTo(ReportDataStatus.NO_DATA);
            assertThat(response.topFailureReasons()).isEmpty();
        }
    }

    @Nested
    @DisplayName("AI 피드백 조회 테스트")
    class AiFeedbackTest {

        @Test
        @DisplayName("AI 분석 결과가 존재할 때")
        void getAiFeedbackWhenExists() {
            // given
            User user = User.builder().userId(userId).build();
            WeeklyAiAnalysis analysis = WeeklyAiAnalysis.builder()
                    .user(user)
                    .weekStartDate(monday)
                    .failureReasonRankingJson(null)
                    .weeklyFeedback("이번주는 잘하셨어요.")
                    .build();

            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(
                    anyLong(), any(), any()))
                    .willReturn(List.of());
            given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(userId, monday))
                    .willReturn(Optional.of(analysis));

            // when
            WeeklyReportResponse response = weeklyReportService.getWeeklyReport(userId, year, month, weekOfMonth);

            // then
            assertThat(response.aiFeedback().failureReasonRanking()).isEmpty();
            assertThat(response.aiFeedback().weeklyFeedback()).isEqualTo("이번주는 잘하셨어요.");
            assertThat(response.aiFeedback().isDefault()).isFalse();
        }

        @Test
        @DisplayName("AI 분석 결과가 없을 때 기본 메시지 반환")
        void getDefaultMessageWhenNoAiFeedback() {
            // given
            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(
                    anyLong(), any(), any()))
                    .willReturn(List.of());
            given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(userId, monday))
                    .willReturn(Optional.empty());

            // when
            WeeklyReportResponse response = weeklyReportService.getWeeklyReport(userId, year, month, weekOfMonth);

            // then
            assertThat(response.aiFeedback().failureReasonRanking()).isEmpty();
            assertThat(response.aiFeedback().weeklyFeedback()).isNotNull();
            assertThat(response.aiFeedback().isDefault()).isTrue();
        }

        @Test
        @DisplayName("failureReasonRankingJson이 유효한 JSON일 때 파싱")
        void parseFailureReasonRankingJson() throws Exception {
            // given
            User user = User.builder().userId(userId).build();
            String rankingJson = "[{\"rank\":1,\"category\":\"시간 부족\",\"count\":3}," +
                    "{\"rank\":2,\"category\":\"피로\",\"count\":2}]";

            WeeklyAiAnalysis analysis = WeeklyAiAnalysis.builder()
                    .user(user)
                    .weekStartDate(monday)
                    .failureReasonRankingJson(rankingJson)
                    .weeklyFeedback("이번주 피드백")
                    .build();

            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(
                    anyLong(), any(), any()))
                    .willReturn(List.of());
            given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(userId, monday))
                    .willReturn(Optional.of(analysis));
            given(objectMapper.readValue(eq(rankingJson), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                    .willReturn(List.of(
                            createFailureReasonRank(1, "시간 부족", 3),
                            createFailureReasonRank(2, "피로", 2)
                    ));

            // when
            WeeklyReportResponse response = weeklyReportService.getWeeklyReport(userId, year, month, weekOfMonth);

            // then
            assertThat(response.dataStatus()).isEqualTo(ReportDataStatus.READY);
            assertThat(response.aiFeedback().failureReasonRanking()).hasSize(2);
            assertThat(response.aiFeedback().failureReasonRanking().get(0).rank()).isEqualTo(1);
            assertThat(response.aiFeedback().failureReasonRanking().get(0).category()).isEqualTo("시간 부족");
            assertThat(response.aiFeedback().failureReasonRanking().get(0).count()).isEqualTo(3);
            assertThat(response.aiFeedback().failureReasonRanking().get(1).rank()).isEqualTo(2);
            assertThat(response.aiFeedback().failureReasonRanking().get(1).category()).isEqualTo("피로");
            assertThat(response.aiFeedback().failureReasonRanking().get(1).count()).isEqualTo(2);
        }

        @Test
        @DisplayName("NO_DATA 상태: 미션 데이터 없고 AI 분석 없을 때")
        void noDataStatusWhenNoMissionDataAndNoAiAnalysis() {
            // given
            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(
                    anyLong(), any(), any()))
                    .willReturn(List.of());
            given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(userId, monday))
                    .willReturn(Optional.empty());

            // when
            WeeklyReportResponse response = weeklyReportService.getWeeklyReport(userId, year, month, weekOfMonth);

            // then
            assertThat(response.dataStatus()).isEqualTo(ReportDataStatus.NO_DATA);
            assertThat(response.aiFeedback().isDefault()).isTrue();
            assertThat(response.aiFeedback().weeklyFeedback()).isEqualTo(DefaultReportMessages.WEEKLY_NO_DATA);
        }

        @Test
        @DisplayName("PENDING_ANALYSIS 상태: 미션 데이터는 있으나 AI 분석 없을 때")
        void pendingAnalysisStatusWhenHasMissionDataButNoAiAnalysis() {
            // given
            List<DailyMissionResult> results = List.of(
                    createMissionResult(monday, MissionResult.SUCCESS),
                    createMissionResult(monday.plusDays(1), MissionResult.FAILURE)
            );

            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(
                    eq(userId), eq(monday), eq(monday.plusDays(6))))
                    .willReturn(results);
            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(
                    eq(userId), eq(monday.minusDays(7)), eq(monday.minusDays(1))))
                    .willReturn(List.of());
            given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(userId, monday))
                    .willReturn(Optional.empty());

            // when
            WeeklyReportResponse response = weeklyReportService.getWeeklyReport(userId, year, month, weekOfMonth);

            // then
            assertThat(response.dataStatus()).isEqualTo(ReportDataStatus.PENDING_ANALYSIS);
            assertThat(response.aiFeedback().isDefault()).isTrue();
            assertThat(response.aiFeedback().weeklyFeedback()).isEqualTo(DefaultReportMessages.WEEKLY_PENDING);
        }

        @Test
        @DisplayName("READY 상태: AI 분석이 존재할 때")
        void readyStatusWhenAiAnalysisExists() {
            // given
            User user = User.builder().userId(userId).build();
            WeeklyAiAnalysis analysis = WeeklyAiAnalysis.builder()
                    .user(user)
                    .weekStartDate(monday)
                    .failureReasonRankingJson(null)
                    .weeklyFeedback("AI 분석 피드백입니다.")
                    .build();

            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(
                    anyLong(), any(), any()))
                    .willReturn(List.of());
            given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(userId, monday))
                    .willReturn(Optional.of(analysis));

            // when
            WeeklyReportResponse response = weeklyReportService.getWeeklyReport(userId, year, month, weekOfMonth);

            // then
            assertThat(response.dataStatus()).isEqualTo(ReportDataStatus.READY);
            assertThat(response.aiFeedback().isDefault()).isFalse();
            assertThat(response.aiFeedback().weeklyFeedback()).isEqualTo("AI 분석 피드백입니다.");
        }
    }

    @Nested
    @DisplayName("주간 시작일 계산 테스트")
    class WeekStartDateTest {

        @Test
        @DisplayName("weekStartDate가 null일 때 이번 주 월요일 반환")
        void resolveWeekStartDateWhenNull() {
            // given
            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(
                    anyLong(), any(), any()))
                    .willReturn(List.of());
            given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(anyLong(), any()))
                    .willReturn(Optional.empty());

            // when
            WeeklyReportResponse response = weeklyReportService.getWeeklyReport(userId, null, null, null);

            // then
            assertThat(response.weekStartDate().getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        }

        @Test
        @DisplayName("year, month, weekOfMonth로 해당 월의 특정 주 월요일 계산")
        void resolveWeekStartDateWithYearMonthWeek() {
            // given
            // 2024년 2월 1일은 목요일, 첫 번째 월요일은 2024-02-05
            // 2주차 월요일은 2024-02-12
            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(
                    anyLong(), any(), any()))
                    .willReturn(List.of());
            given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(anyLong(), any()))
                    .willReturn(Optional.empty());

            // when
            WeeklyReportResponse response = weeklyReportService.getWeeklyReport(userId, 2024, 2, 2);

            // then
            assertThat(response.weekStartDate()).isEqualTo(LocalDate.of(2024, 2, 12));
            assertThat(response.weekEndDate()).isEqualTo(LocalDate.of(2024, 2, 18));
        }

        @Test
        @DisplayName("존재하지 않는 주차 요청 시 예외 발생")
        void throwExceptionWhenInvalidWeekOfMonth() {
            // given & when & then
            // 2024년 2월은 최대 4주차까지 존재 (첫 번째 월요일: 2/5, 4주차 월요일: 2/26)
            assertThatThrownBy(() -> weeklyReportService.getWeeklyReport(userId, 2024, 2, 15))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INVALID_WEEK_OF_MONTH);
                    });
        }

        @Test
        @DisplayName("0 이하의 주차 요청 시 예외 발생")
        void throwExceptionWhenWeekOfMonthIsZeroOrNegative() {
            // given & when & then
            assertThatThrownBy(() -> weeklyReportService.getWeeklyReport(userId, 2024, 2, 0))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INVALID_WEEK_OF_MONTH);
                    });

            assertThatThrownBy(() -> weeklyReportService.getWeeklyReport(userId, 2024, 2, -1))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INVALID_WEEK_OF_MONTH);
                    });
        }
    }

    private DailyMissionResult createMissionResult(LocalDate date, MissionResult result) {
        return createMissionResult(date, result, MissionType.EXERCISE);
    }

    private DailyMissionResult createMissionResult(LocalDate date, MissionResult result, MissionType type) {
        Mission mission = Mission.builder()
                .name("테스트 미션")
                .type(type)
                .build();

        return DailyMissionResult.builder()
                .missionDate(date)
                .result(result)
                .mission(mission)
                .build();
    }

    private DailyMissionResult createMissionResultWithReason(LocalDate date, String failureReason) {
        Mission mission = Mission.builder()
                .name("테스트 미션")
                .type(MissionType.EXERCISE)
                .build();

        return DailyMissionResult.builder()
                .missionDate(date)
                .result(MissionResult.FAILURE)
                .failureReason(failureReason)
                .mission(mission)
                .build();
    }

    private AiWeeklyAnalysisResponse.FailureReasonRank createFailureReasonRank(int rank, String category, int count) {
        AiWeeklyAnalysisResponse.FailureReasonRank failureReasonRank =
                new AiWeeklyAnalysisResponse.FailureReasonRank();
        failureReasonRank.setRank(rank);
        failureReasonRank.setCategory(category);
        failureReasonRank.setCount(count);
        return failureReasonRank;
    }
}
