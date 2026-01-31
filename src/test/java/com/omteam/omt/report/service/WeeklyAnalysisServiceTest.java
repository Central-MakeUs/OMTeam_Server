package com.omteam.omt.report.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omteam.omt.common.ai.dto.UserContext;
import com.omteam.omt.common.ai.service.UserContextService;
import com.omteam.omt.mission.domain.DailyMissionResult;
import com.omteam.omt.mission.domain.MissionResult;
import com.omteam.omt.mission.repository.DailyMissionResultRepository;
import com.omteam.omt.report.client.AiWeeklyAnalysisClient;
import com.omteam.omt.report.client.dto.AiWeeklyAnalysisRequest;
import com.omteam.omt.report.client.dto.AiWeeklyAnalysisResponse;
import com.omteam.omt.report.domain.WeeklyAiAnalysis;
import com.omteam.omt.report.repository.WeeklyAiAnalysisRepository;
import com.omteam.omt.user.domain.User;
import com.omteam.omt.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WeeklyAnalysisServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    DailyMissionResultRepository missionResultRepository;

    @Mock
    WeeklyAiAnalysisRepository weeklyAiAnalysisRepository;

    @Mock
    AiWeeklyAnalysisClient aiWeeklyAnalysisClient;

    @Mock
    UserContextService userContextService;

    @Mock
    ObjectMapper objectMapper;

    WeeklyAnalysisService weeklyAnalysisService;

    final LocalDate monday = LocalDate.of(2024, 1, 15);

    @BeforeEach
    void setUp() {
        weeklyAnalysisService = new WeeklyAnalysisService(
                userRepository,
                missionResultRepository,
                weeklyAiAnalysisRepository,
                aiWeeklyAnalysisClient,
                userContextService,
                objectMapper
        );
    }

    @Nested
    @DisplayName("전체 사용자 주간 분석 테스트")
    class GenerateForAllUsersTest {

        @Test
        @DisplayName("모든 활성 사용자에 대해 분석 실행")
        void generateWeeklyAnalysisForAllUsers() {
            // given
            User user1 = User.builder().userId(1L).build();
            User user2 = User.builder().userId(2L).build();

            UserContext mockUserContext = UserContext.builder()
                    .nickname("테스트 사용자")
                    .appGoal("건강 증진")
                    .currentLevel(1)
                    .build();

            given(userRepository.findAllByDeletedAtIsNull()).willReturn(List.of(user1, user2));
            given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(anyLong(), any()))
                    .willReturn(Optional.empty());
            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(anyLong(), any(), any()))
                    .willReturn(List.of());
            given(userContextService.buildContext(anyLong())).willReturn(mockUserContext);

            AiWeeklyAnalysisResponse aiResponse = new AiWeeklyAnalysisResponse();
            aiResponse.setMainFailureReason("테스트");
            aiResponse.setOverallFeedback("피드백");
            given(aiWeeklyAnalysisClient.analyzeWeeklyMissions(any())).willReturn(aiResponse);

            // when
            weeklyAnalysisService.generateWeeklyAnalysisForAllUsers(monday);

            // then
            verify(aiWeeklyAnalysisClient, times(2)).analyzeWeeklyMissions(any());
            verify(weeklyAiAnalysisRepository, times(2)).save(any(WeeklyAiAnalysis.class));
        }

        @Test
        @DisplayName("개별 사용자 실패가 전체에 영향 없음")
        void individualFailureDoesNotAffectOthers() {
            // given
            User user1 = User.builder().userId(1L).build();
            User user2 = User.builder().userId(2L).build();

            UserContext mockUserContext = UserContext.builder()
                    .nickname("테스트 사용자")
                    .appGoal("건강 증진")
                    .currentLevel(1)
                    .build();

            given(userRepository.findAllByDeletedAtIsNull()).willReturn(List.of(user1, user2));
            given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(anyLong(), any()))
                    .willReturn(Optional.empty());
            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(anyLong(), any(), any()))
                    .willReturn(List.of());
            given(userContextService.buildContext(anyLong())).willReturn(mockUserContext);

            AiWeeklyAnalysisResponse aiResponse = new AiWeeklyAnalysisResponse();
            aiResponse.setMainFailureReason("테스트");
            aiResponse.setOverallFeedback("피드백");

            // 첫 번째 사용자 실패, 두 번째 사용자 성공
            given(aiWeeklyAnalysisClient.analyzeWeeklyMissions(any()))
                    .willThrow(new RuntimeException("AI 서버 오류"))
                    .willReturn(aiResponse);

            // when - 예외 발생하지 않아야 함
            assertThatCode(() -> weeklyAnalysisService.generateWeeklyAnalysisForAllUsers(monday))
                    .doesNotThrowAnyException();

            // then - 두 번째 사용자는 정상 처리
            verify(weeklyAiAnalysisRepository, times(1)).save(any(WeeklyAiAnalysis.class));
        }
    }

    @Nested
    @DisplayName("개별 사용자 주간 분석 테스트")
    class GenerateForUserTest {

        @Test
        @DisplayName("이미 분석 결과가 있으면 스킵")
        void skipWhenAnalysisExists() {
            // given
            User user = User.builder().userId(1L).build();
            WeeklyAiAnalysis existingAnalysis = WeeklyAiAnalysis.builder()
                    .user(user)
                    .weekStartDate(monday)
                    .build();

            given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(1L, monday))
                    .willReturn(Optional.of(existingAnalysis));

            // when
            weeklyAnalysisService.generateWeeklyAnalysisForUser(user, monday, monday.plusDays(6));

            // then
            verify(aiWeeklyAnalysisClient, never()).analyzeWeeklyMissions(any());
            verify(weeklyAiAnalysisRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패 사유 수집 및 AI 요청")
        void collectFailureReasonsAndCallAi() {
            // given
            User user = User.builder().userId(1L).build();
            LocalDate weekEnd = monday.plusDays(6);

            UserContext mockUserContext = UserContext.builder()
                    .nickname("테스트 사용자")
                    .appGoal("건강 증진")
                    .currentLevel(1)
                    .build();

            List<DailyMissionResult> results = List.of(
                    DailyMissionResult.builder()
                            .missionDate(monday)
                            .result(MissionResult.FAILURE)
                            .failureReason("시간 부족")
                            .build(),
                    DailyMissionResult.builder()
                            .missionDate(monday.plusDays(1))
                            .result(MissionResult.FAILURE)
                            .failureReason("피로")
                            .build(),
                    DailyMissionResult.builder()
                            .missionDate(monday.plusDays(2))
                            .result(MissionResult.SUCCESS)
                            .build()
            );

            given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(1L, monday))
                    .willReturn(Optional.empty());
            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(1L, monday, weekEnd))
                    .willReturn(results);
            given(userContextService.buildContext(1L)).willReturn(mockUserContext);

            AiWeeklyAnalysisResponse aiResponse = new AiWeeklyAnalysisResponse();
            aiResponse.setMainFailureReason("시간 부족");
            aiResponse.setOverallFeedback("피드백");
            given(aiWeeklyAnalysisClient.analyzeWeeklyMissions(any())).willReturn(aiResponse);

            // when
            weeklyAnalysisService.generateWeeklyAnalysisForUser(user, monday, weekEnd);

            // then
            ArgumentCaptor<AiWeeklyAnalysisRequest> requestCaptor =
                    ArgumentCaptor.forClass(AiWeeklyAnalysisRequest.class);
            verify(aiWeeklyAnalysisClient).analyzeWeeklyMissions(requestCaptor.capture());

            AiWeeklyAnalysisRequest capturedRequest = requestCaptor.getValue();
            assertThat(capturedRequest.getFailureReasons()).containsExactlyInAnyOrder("시간 부족", "피로");
        }

        @Test
        @DisplayName("분석 결과 저장")
        void saveAnalysisResult() {
            // given
            User user = User.builder().userId(1L).build();
            LocalDate weekEnd = monday.plusDays(6);

            UserContext mockUserContext = UserContext.builder()
                    .nickname("테스트 사용자")
                    .appGoal("건강 증진")
                    .currentLevel(1)
                    .build();

            given(weeklyAiAnalysisRepository.findByUserUserIdAndWeekStartDate(1L, monday))
                    .willReturn(Optional.empty());
            given(missionResultRepository.findByUserUserIdAndMissionDateBetween(1L, monday, weekEnd))
                    .willReturn(List.of());
            given(userContextService.buildContext(1L)).willReturn(mockUserContext);

            AiWeeklyAnalysisResponse aiResponse = new AiWeeklyAnalysisResponse();
            aiResponse.setMainFailureReason("시간 부족");
            aiResponse.setOverallFeedback("종합 피드백");
            given(aiWeeklyAnalysisClient.analyzeWeeklyMissions(any())).willReturn(aiResponse);

            // when
            weeklyAnalysisService.generateWeeklyAnalysisForUser(user, monday, weekEnd);

            // then
            ArgumentCaptor<WeeklyAiAnalysis> analysisCaptor =
                    ArgumentCaptor.forClass(WeeklyAiAnalysis.class);
            verify(weeklyAiAnalysisRepository).save(analysisCaptor.capture());

            WeeklyAiAnalysis savedAnalysis = analysisCaptor.getValue();
            assertThat(savedAnalysis.getUser()).isEqualTo(user);
            assertThat(savedAnalysis.getWeekStartDate()).isEqualTo(monday);
            assertThat(savedAnalysis.getMainFailureReason()).isEqualTo("시간 부족");
            assertThat(savedAnalysis.getOverallFeedback()).isEqualTo("종합 피드백");
        }
    }
}
