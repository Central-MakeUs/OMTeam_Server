package com.omteam.omt.mission.validator;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import com.omteam.omt.common.exception.BusinessException;
import com.omteam.omt.common.exception.ErrorCode;
import com.omteam.omt.mission.domain.DailyRecommendedMission;
import com.omteam.omt.mission.domain.RecommendedMissionStatus;
import com.omteam.omt.mission.repository.DailyMissionResultRepository;
import com.omteam.omt.mission.repository.DailyRecommendedMissionRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("[단위] MissionStatusValidator")
class MissionStatusValidatorTest {

    @Mock
    DailyRecommendedMissionRepository recommendedMissionRepository;

    @Mock
    DailyMissionResultRepository missionResultRepository;

    @InjectMocks
    MissionStatusValidator missionStatusValidator;

    private static final Long USER_ID = 1L;
    private static final LocalDate TODAY = LocalDate.now();

    @Nested
    @DisplayName("validateNoMissionResultToday")
    class ValidateNoMissionResultToday {

        @Test
        @DisplayName("오늘 미션 결과가 없으면 예외 없이 통과한다")
        void success_noResultExists() {
            // given
            given(missionResultRepository.existsByUserUserIdAndMissionDate(USER_ID, TODAY))
                    .willReturn(false);

            // when & then
            assertThatCode(() -> missionStatusValidator.validateNoMissionResultToday(USER_ID, TODAY))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("오늘 미션 결과가 있으면 DAILY_MISSION_ALREADY_EXISTS 예외가 발생한다")
        void fail_resultAlreadyExists() {
            // given
            given(missionResultRepository.existsByUserUserIdAndMissionDate(USER_ID, TODAY))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> missionStatusValidator.validateNoMissionResultToday(USER_ID, TODAY))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.DAILY_MISSION_ALREADY_EXISTS);
        }
    }

    @Nested
    @DisplayName("validateNoMissionResultTodayForComplete")
    class ValidateNoMissionResultTodayForComplete {

        @Test
        @DisplayName("오늘 미션 결과가 없으면 예외 없이 통과한다")
        void success_noResultExists() {
            // given
            given(missionResultRepository.existsByUserUserIdAndMissionDate(USER_ID, TODAY))
                    .willReturn(false);

            // when & then
            assertThatCode(() -> missionStatusValidator.validateNoMissionResultTodayForComplete(USER_ID, TODAY))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("오늘 미션 결과가 있으면 MISSION_RESULT_ALREADY_EXISTS 예외가 발생한다")
        void fail_resultAlreadyExists() {
            // given
            given(missionResultRepository.existsByUserUserIdAndMissionDate(USER_ID, TODAY))
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() -> missionStatusValidator.validateNoMissionResultTodayForComplete(USER_ID, TODAY))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.MISSION_RESULT_ALREADY_EXISTS);
        }
    }

    @Nested
    @DisplayName("validateMissionStartable")
    class ValidateMissionStartable {

        @Test
        @DisplayName("RECOMMENDED 상태의 미션이면 예외 없이 통과한다")
        void success_recommended() {
            // given
            DailyRecommendedMission mission = DailyRecommendedMission.builder()
                    .status(RecommendedMissionStatus.RECOMMENDED)
                    .build();

            // when & then
            assertThatCode(() -> missionStatusValidator.validateMissionStartable(mission))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("IN_PROGRESS 상태의 미션이면 INVALID_MISSION_STATUS 예외가 발생한다")
        void fail_inProgress() {
            // given
            DailyRecommendedMission mission = DailyRecommendedMission.builder()
                    .status(RecommendedMissionStatus.IN_PROGRESS)
                    .build();

            // when & then
            assertThatThrownBy(() -> missionStatusValidator.validateMissionStartable(mission))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_MISSION_STATUS);
        }

        @Test
        @DisplayName("COMPLETED 상태의 미션이면 INVALID_MISSION_STATUS 예외가 발생한다")
        void fail_completed() {
            // given
            DailyRecommendedMission mission = DailyRecommendedMission.builder()
                    .status(RecommendedMissionStatus.COMPLETED)
                    .build();

            // when & then
            assertThatThrownBy(() -> missionStatusValidator.validateMissionStartable(mission))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_MISSION_STATUS);
        }

        @Test
        @DisplayName("EXPIRED 상태의 미션이면 INVALID_MISSION_STATUS 예외가 발생한다")
        void fail_expired() {
            // given
            DailyRecommendedMission mission = DailyRecommendedMission.builder()
                    .status(RecommendedMissionStatus.EXPIRED)
                    .build();

            // when & then
            assertThatThrownBy(() -> missionStatusValidator.validateMissionStartable(mission))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_MISSION_STATUS);
        }
    }

    @Nested
    @DisplayName("getInProgressMissionOrThrow")
    class GetInProgressMissionOrThrow {

        @Test
        @DisplayName("진행 중인 미션이 있으면 해당 미션을 반환한다")
        void success_missionExists() {
            // given
            DailyRecommendedMission inProgressMission = DailyRecommendedMission.builder()
                    .missionDate(TODAY)
                    .status(RecommendedMissionStatus.IN_PROGRESS)
                    .build();

            given(recommendedMissionRepository.findByUserUserIdAndMissionDateAndStatus(
                    USER_ID, TODAY, RecommendedMissionStatus.IN_PROGRESS))
                    .willReturn(List.of(inProgressMission));

            // when
            DailyRecommendedMission result = missionStatusValidator.getInProgressMissionOrThrow(USER_ID, TODAY);

            // then
            assertThat(result).isEqualTo(inProgressMission);
            assertThat(result.getStatus()).isEqualTo(RecommendedMissionStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("진행 중인 미션이 없으면 MISSION_NOT_IN_PROGRESS 예외가 발생한다")
        void fail_noInProgressMission() {
            // given
            given(recommendedMissionRepository.findByUserUserIdAndMissionDateAndStatus(
                    USER_ID, TODAY, RecommendedMissionStatus.IN_PROGRESS))
                    .willReturn(List.of());

            // when & then
            assertThatThrownBy(() -> missionStatusValidator.getInProgressMissionOrThrow(USER_ID, TODAY))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.MISSION_NOT_IN_PROGRESS);
        }

        @Test
        @DisplayName("진행 중인 미션이 여러 개면 첫 번째를 반환한다")
        void success_multipleInProgress_returnsFirst() {
            // given
            DailyRecommendedMission first = DailyRecommendedMission.builder()
                    .missionDate(TODAY)
                    .status(RecommendedMissionStatus.IN_PROGRESS)
                    .build();
            DailyRecommendedMission second = DailyRecommendedMission.builder()
                    .missionDate(TODAY)
                    .status(RecommendedMissionStatus.IN_PROGRESS)
                    .build();

            given(recommendedMissionRepository.findByUserUserIdAndMissionDateAndStatus(
                    USER_ID, TODAY, RecommendedMissionStatus.IN_PROGRESS))
                    .willReturn(List.of(first, second));

            // when
            DailyRecommendedMission result = missionStatusValidator.getInProgressMissionOrThrow(USER_ID, TODAY);

            // then
            assertThat(result).isEqualTo(first);
        }
    }
}
