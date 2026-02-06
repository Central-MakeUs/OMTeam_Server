package com.omteam.omt.integration.mission;

import static org.assertj.core.api.Assertions.*;

import com.omteam.omt.integration.IntegrationTestBase;
import com.omteam.omt.mission.domain.DailyRecommendedMission;
import com.omteam.omt.mission.domain.Mission;
import com.omteam.omt.mission.domain.MissionType;
import com.omteam.omt.mission.domain.RecommendedMissionStatus;
import com.omteam.omt.mission.dto.DailyMissionRecommendResponse;
import com.omteam.omt.mission.repository.DailyRecommendedMissionRepository;
import com.omteam.omt.mission.repository.MissionRepository;
import com.omteam.omt.mission.service.MissionService;
import com.omteam.omt.user.domain.LifestyleType;
import com.omteam.omt.user.domain.User;
import com.omteam.omt.user.domain.UserOnboarding;
import com.omteam.omt.user.domain.WorkTimeType;
import com.omteam.omt.user.repository.UserOnboardingRepository;
import com.omteam.omt.user.repository.UserRepository;
import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@DisplayName("[통합] MissionService - AI 미션 추천 및 DB 저장")
@Transactional
class MissionServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MissionService missionService;

    @Autowired
    private MissionRepository missionRepository;

    @Autowired
    private DailyRecommendedMissionRepository recommendedMissionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserOnboardingRepository userOnboardingRepository;

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
                .preferredExercises(List.of("스트레칭", "걷기"))
                .lifestyleType(LifestyleType.REGULAR_DAYTIME)
                .build());
    }

    @Test
    @DisplayName("AI 서버에서 추천받은 미션이 DB에 정상 저장되고 조회된다")
    void recommendDailyMissions_savesAndRetrievesFromDb() throws Exception {
        // given
        String aiResponse = """
            {
                "missions": [
                    {
                        "name": "스트레칭 10분",
                        "type": "EXERCISE",
                        "difficulty": 1,
                        "estimatedMinutes": 10,
                        "estimatedCalories": 30
                    },
                    {
                        "name": "계단 오르기 5층",
                        "type": "EXERCISE",
                        "difficulty": 2,
                        "estimatedMinutes": 15,
                        "estimatedCalories": 50
                    },
                    {
                        "name": "점심 샐러드 먹기",
                        "type": "DIET",
                        "difficulty": 2,
                        "estimatedMinutes": 30,
                        "estimatedCalories": 0
                    }
                ]
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(aiResponse));

        // when
        DailyMissionRecommendResponse response = missionService.recommendDailyMissions(testUser.getUserId());

        // then - 응답 검증
        assertThat(response).isNotNull();
        assertThat(response.getRecommendations()).hasSize(3);
        assertThat(response.getMissionDate()).isEqualTo(LocalDate.now());

        // then - Mission 엔티티 DB 저장 검증
        List<Mission> savedMissions = missionRepository.findAll();
        assertThat(savedMissions).hasSize(3);

        Mission exerciseMission = savedMissions.stream()
                .filter(m -> m.getName().equals("스트레칭 10분"))
                .findFirst()
                .orElseThrow();
        assertThat(exerciseMission.getType()).isEqualTo(MissionType.EXERCISE);
        assertThat(exerciseMission.getDifficulty()).isEqualTo(1);
        assertThat(exerciseMission.getEstimatedMinutes()).isEqualTo(10);
        assertThat(exerciseMission.getEstimatedCalories()).isEqualTo(30);

        Mission dietMission = savedMissions.stream()
                .filter(m -> m.getType() == MissionType.DIET)
                .findFirst()
                .orElseThrow();
        assertThat(dietMission.getName()).isEqualTo("점심 샐러드 먹기");

        // then - DailyRecommendedMission 엔티티 DB 저장 검증
        List<DailyRecommendedMission> recommendations = recommendedMissionRepository.findAll();
        assertThat(recommendations).hasSize(3);
        assertThat(recommendations).allMatch(r -> r.getStatus() == RecommendedMissionStatus.RECOMMENDED);
        assertThat(recommendations).allMatch(r -> r.getMissionDate().equals(LocalDate.now()));
        assertThat(recommendations).allMatch(r -> r.getUser().getUserId().equals(testUser.getUserId()));
    }

    @Test
    @DisplayName("저장된 추천 미션을 사용자별로 조회할 수 있다")
    void getTodayRecommendations_returnsFromDb() throws Exception {
        // given
        String aiResponse = """
            {
                "missions": [
                    {
                        "name": "아침 산책 20분",
                        "type": "EXERCISE",
                        "difficulty": 2,
                        "estimatedMinutes": 20,
                        "estimatedCalories": 60
                    }
                ]
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(aiResponse));

        missionService.recommendDailyMissions(testUser.getUserId());

        // when
        var recommendations = missionService.getTodayRecommendations(testUser.getUserId());

        // then
        assertThat(recommendations).hasSize(1);
        assertThat(recommendations.get(0).getMission().getName()).isEqualTo("아침 산책 20분");
        assertThat(recommendations.get(0).getMission().getType()).isEqualTo(MissionType.EXERCISE);
        assertThat(recommendations.get(0).getMission().getDifficulty()).isEqualTo(2);
    }

    @Test
    @DisplayName("미션 시작 후 상태가 IN_PROGRESS로 변경되어 DB에 저장된다")
    void startMission_updatesStatusInDb() throws Exception {
        // given
        String aiResponse = """
            {
                "missions": [
                    {
                        "name": "팔굽혀펴기 10회",
                        "type": "EXERCISE",
                        "difficulty": 3,
                        "estimatedMinutes": 5,
                        "estimatedCalories": 25
                    }
                ]
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(aiResponse));

        var recommendResponse = missionService.recommendDailyMissions(testUser.getUserId());
        Long recommendedMissionId = recommendResponse.getRecommendations().get(0).getRecommendedMissionId();

        // when
        missionService.startMission(testUser.getUserId(), recommendedMissionId);

        // then
        DailyRecommendedMission updatedMission = recommendedMissionRepository.findById(recommendedMissionId)
                .orElseThrow();
        assertThat(updatedMission.getStatus()).isEqualTo(RecommendedMissionStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("오늘의 미션 상태를 조회하면 DB에서 올바른 데이터를 반환한다")
    void getTodayMissionStatus_returnsCorrectData() throws Exception {
        // given
        String aiResponse = """
            {
                "missions": [
                    {
                        "name": "물 2L 마시기",
                        "type": "DIET",
                        "difficulty": 1,
                        "estimatedMinutes": 0,
                        "estimatedCalories": 0
                    }
                ]
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(aiResponse));

        missionService.recommendDailyMissions(testUser.getUserId());

        // when
        var status = missionService.getTodayMissionStatus(testUser.getUserId());

        // then
        assertThat(status.isHasRecommendations()).isTrue();
        assertThat(status.isHasInProgressMission()).isFalse();
        assertThat(status.isHasCompletedMission()).isFalse();
        assertThat(status.getDate()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("난이도 1~5 범위의 미션이 정상 저장된다")
    void recommendDailyMissions_savesDifficultyRange() throws Exception {
        // given
        String aiResponse = """
            {
                "missions": [
                    {
                        "name": "쉬운 미션",
                        "type": "EXERCISE",
                        "difficulty": 1,
                        "estimatedMinutes": 5,
                        "estimatedCalories": 10
                    },
                    {
                        "name": "어려운 미션",
                        "type": "EXERCISE",
                        "difficulty": 5,
                        "estimatedMinutes": 60,
                        "estimatedCalories": 300
                    }
                ]
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(aiResponse));

        // when
        missionService.recommendDailyMissions(testUser.getUserId());

        // then
        List<Mission> missions = missionRepository.findAll();
        assertThat(missions).extracting(Mission::getDifficulty).containsExactlyInAnyOrder(1, 5);
    }
}
