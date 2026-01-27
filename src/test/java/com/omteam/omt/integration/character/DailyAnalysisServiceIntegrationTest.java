package com.omteam.omt.integration.character;

import static org.assertj.core.api.Assertions.*;

import com.omteam.omt.character.domain.DailyAnalysis;
import com.omteam.omt.character.domain.EncouragementIntent;
import com.omteam.omt.character.repository.DailyAnalysisRepository;
import com.omteam.omt.character.service.DailyAnalysisService;
import com.omteam.omt.integration.IntegrationTestBase;
import com.omteam.omt.mission.domain.DailyMissionResult;
import com.omteam.omt.mission.domain.Mission;
import com.omteam.omt.mission.domain.MissionResult;
import com.omteam.omt.mission.domain.MissionType;
import com.omteam.omt.mission.repository.DailyMissionResultRepository;
import com.omteam.omt.mission.repository.MissionRepository;
import com.omteam.omt.user.domain.LifestyleType;
import com.omteam.omt.user.domain.User;
import com.omteam.omt.user.domain.UserOnboarding;
import com.omteam.omt.user.domain.WorkTimeType;
import com.omteam.omt.user.repository.UserOnboardingRepository;
import com.omteam.omt.user.repository.UserRepository;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@DisplayName("[통합] DailyAnalysisService - AI 일일 분석 및 DB 저장")
@Transactional
class DailyAnalysisServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private DailyAnalysisService dailyAnalysisService;

    @Autowired
    private DailyAnalysisRepository dailyAnalysisRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserOnboardingRepository userOnboardingRepository;

    @Autowired
    private MissionRepository missionRepository;

    @Autowired
    private DailyMissionResultRepository missionResultRepository;

    private User testUser;

    @BeforeEach
    void setUpTestData() {
        testUser = userRepository.save(User.builder()
                .email("test@example.com")
                .nickname("테스트유저")
                .onboardingCompleted(true)
                .build());

        userOnboardingRepository.save(UserOnboarding.builder()
                .user(testUser)
                .appGoalText("체중 감량")
                .workTimeType(WorkTimeType.FIXED)
                .availableStartTime(LocalTime.of(18, 0))
                .availableEndTime(LocalTime.of(21, 0))
                .minExerciseMinutes(30)
                .preferredExerciseText("스트레칭, 걷기")
                .lifestyleType(LifestyleType.REGULAR_DAYTIME)
                .build());
    }

    @Test
    @DisplayName("AI 서버에서 받은 일일 분석 결과가 DB에 정상 저장되고 조회된다")
    void generateDailyAnalysisForUser_savesAndRetrievesFromDb() throws Exception {
        // given
        String aiResponse = """
            {
                "feedbackText": "오늘도 꾸준히 운동을 완료했네요! 이대로 가면 목표 달성이 가능해요.",
                "encouragementCandidates": [
                    {
                        "intent": "PRAISE",
                        "title": "대단해요!",
                        "message": "연속 3일째 미션 성공이에요."
                    },
                    {
                        "intent": "RETRY",
                        "title": "다시 시작해봐요",
                        "message": "어제는 쉬었지만 오늘은 다시 시작할 수 있어요."
                    },
                    {
                        "intent": "NORMAL",
                        "title": "꾸준함이 중요해요",
                        "message": "작은 습관이 큰 변화를 만들어요."
                    },
                    {
                        "intent": "PUSH",
                        "title": "오늘 시작해볼까요?",
                        "message": "지금 바로 시작해봐요!"
                    }
                ]
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(aiResponse));

        LocalDate targetDate = LocalDate.now();

        // when
        dailyAnalysisService.generateDailyAnalysisForUser(testUser, targetDate);

        // then - DB 저장 검증
        Optional<DailyAnalysis> savedAnalysis = dailyAnalysisRepository
                .findByUserUserIdAndTargetDate(testUser.getUserId(), targetDate);

        assertThat(savedAnalysis).isPresent();

        DailyAnalysis analysis = savedAnalysis.get();
        assertThat(analysis.getFeedbackText()).contains("꾸준히 운동을 완료");
        assertThat(analysis.getTargetDate()).isEqualTo(targetDate);
        assertThat(analysis.getUser().getUserId()).isEqualTo(testUser.getUserId());

        // PRAISE 메시지 검증
        assertThat(analysis.getPraise()).isNotNull();
        assertThat(analysis.getPraise().getTitle()).isEqualTo("대단해요!");
        assertThat(analysis.getPraise().getMessage()).isEqualTo("연속 3일째 미션 성공이에요.");

        // RETRY 메시지 검증
        assertThat(analysis.getRetry()).isNotNull();
        assertThat(analysis.getRetry().getTitle()).isEqualTo("다시 시작해봐요");

        // NORMAL 메시지 검증
        assertThat(analysis.getNormal()).isNotNull();
        assertThat(analysis.getNormal().getTitle()).isEqualTo("꾸준함이 중요해요");

        // PUSH 메시지 검증
        assertThat(analysis.getPush()).isNotNull();
        assertThat(analysis.getPush().getTitle()).isEqualTo("오늘 시작해볼까요?");
    }

    @Test
    @DisplayName("getMessageByIntent로 intent별 메시지를 조회할 수 있다")
    void getMessageByIntent_returnsCorrectMessage() throws Exception {
        // given
        String aiResponse = """
            {
                "feedbackText": "테스트 피드백",
                "encouragementCandidates": [
                    {
                        "intent": "PRAISE",
                        "title": "칭찬 제목",
                        "message": "칭찬 메시지"
                    },
                    {
                        "intent": "RETRY",
                        "title": "재시도 제목",
                        "message": "재시도 메시지"
                    },
                    {
                        "intent": "NORMAL",
                        "title": "일반 제목",
                        "message": "일반 메시지"
                    },
                    {
                        "intent": "PUSH",
                        "title": "독려 제목",
                        "message": "독려 메시지"
                    }
                ]
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(aiResponse));

        LocalDate targetDate = LocalDate.now();
        dailyAnalysisService.generateDailyAnalysisForUser(testUser, targetDate);

        // when
        DailyAnalysis analysis = dailyAnalysisRepository
                .findByUserUserIdAndTargetDate(testUser.getUserId(), targetDate)
                .orElseThrow();

        // then
        assertThat(analysis.getMessageByIntent(EncouragementIntent.PRAISE).getTitle())
                .isEqualTo("칭찬 제목");
        assertThat(analysis.getMessageByIntent(EncouragementIntent.RETRY).getTitle())
                .isEqualTo("재시도 제목");
        assertThat(analysis.getMessageByIntent(EncouragementIntent.NORMAL).getTitle())
                .isEqualTo("일반 제목");
        assertThat(analysis.getMessageByIntent(EncouragementIntent.PUSH).getTitle())
                .isEqualTo("독려 제목");
    }

    @Test
    @DisplayName("어제 미션 결과가 있으면 AI 요청에 포함된다")
    void generateDailyAnalysisForUser_includesYesterdayMission() throws Exception {
        // given - 어제 미션 결과 생성
        Mission mission = missionRepository.save(Mission.builder()
                .name("스트레칭 10분")
                .type(MissionType.EXERCISE)
                .difficulty(2)
                .estimatedMinutes(10)
                .estimatedCalories(30)
                .build());

        LocalDate yesterday = LocalDate.now().minusDays(1);
        missionResultRepository.save(DailyMissionResult.builder()
                .missionDate(yesterday)
                .result(MissionResult.SUCCESS)
                .failureReason(null)
                .mission(mission)
                .user(testUser)
                .build());

        String aiResponse = """
            {
                "feedbackText": "어제 미션을 성공적으로 완료했네요!",
                "encouragementCandidates": [
                    {
                        "intent": "PRAISE",
                        "title": "잘했어요!",
                        "message": "이 기세를 유지해봐요."
                    }
                ]
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(aiResponse));

        LocalDate targetDate = LocalDate.now();

        // when
        dailyAnalysisService.generateDailyAnalysisForUser(testUser, targetDate);

        // then - 요청 검증
        RecordedRequest request = mockWebServer.takeRequest(5, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        String requestBody = request.getBody().readUtf8();
        assertThat(requestBody).contains("EXERCISE");
        assertThat(requestBody).contains("SUCCESS");
    }

    @Test
    @DisplayName("이미 분석 결과가 있으면 중복 생성하지 않는다")
    void generateDailyAnalysisForUser_skipsIfExists() throws Exception {
        // given - 첫 번째 분석 생성
        String aiResponse = """
            {
                "feedbackText": "첫 번째 피드백",
                "encouragementCandidates": [
                    {
                        "intent": "NORMAL",
                        "title": "첫 번째 제목",
                        "message": "첫 번째 메시지"
                    }
                ]
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(aiResponse));

        LocalDate targetDate = LocalDate.now();
        dailyAnalysisService.generateDailyAnalysisForUser(testUser, targetDate);

        // when - 같은 날짜로 다시 호출
        dailyAnalysisService.generateDailyAnalysisForUser(testUser, targetDate);

        // then - 1개만 저장되어 있어야 함
        long count = dailyAnalysisRepository.findAll().stream()
                .filter(a -> a.getUser().getUserId().equals(testUser.getUserId()))
                .filter(a -> a.getTargetDate().equals(targetDate))
                .count();
        assertThat(count).isEqualTo(1);

        // API는 한 번만 호출되었어야 함
        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("일부 intent만 포함된 응답도 정상 저장된다")
    void generateDailyAnalysisForUser_handlesPartialIntents() throws Exception {
        // given - PRAISE만 포함된 응답
        String aiResponse = """
            {
                "feedbackText": "미션 실패 피드백",
                "encouragementCandidates": [
                    {
                        "intent": "RETRY",
                        "title": "다시 도전해요",
                        "message": "실패는 성공의 어머니예요."
                    }
                ]
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(aiResponse));

        LocalDate targetDate = LocalDate.now();

        // when
        dailyAnalysisService.generateDailyAnalysisForUser(testUser, targetDate);

        // then
        DailyAnalysis analysis = dailyAnalysisRepository
                .findByUserUserIdAndTargetDate(testUser.getUserId(), targetDate)
                .orElseThrow();

        assertThat(analysis.getRetry()).isNotNull();
        assertThat(analysis.getRetry().getTitle()).isEqualTo("다시 도전해요");
        assertThat(analysis.getPraise()).isNull();
        assertThat(analysis.getNormal()).isNull();
        assertThat(analysis.getPush()).isNull();
    }
}
