package com.omteam.omt.mission.validator;

import com.omteam.omt.common.exception.BusinessException;
import com.omteam.omt.common.exception.ErrorCode;
import com.omteam.omt.mission.domain.DailyRecommendedMission;
import com.omteam.omt.mission.domain.RecommendedMissionStatus;
import com.omteam.omt.mission.repository.DailyMissionResultRepository;
import com.omteam.omt.mission.repository.DailyRecommendedMissionRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MissionStatusValidator {

    private final DailyRecommendedMissionRepository recommendedMissionRepository;
    private final DailyMissionResultRepository missionResultRepository;

    public void validateNoMissionResultToday(Long userId, LocalDate date) {
        if (missionResultRepository.existsByUserUserIdAndMissionDate(userId, date)) {
            throw new BusinessException(ErrorCode.DAILY_MISSION_ALREADY_EXISTS);
        }
    }

    public void validateNoMissionResultTodayForComplete(Long userId, LocalDate date) {
        if (missionResultRepository.existsByUserUserIdAndMissionDate(userId, date)) {
            throw new BusinessException(ErrorCode.MISSION_RESULT_ALREADY_EXISTS);
        }
    }

    public void validateMissionStartable(DailyRecommendedMission mission) {
        if (!mission.isStartable()) {
            throw new BusinessException(ErrorCode.INVALID_MISSION_STATUS);
        }
    }

    public DailyRecommendedMission getInProgressMissionOrThrow(Long userId, LocalDate date) {
        return recommendedMissionRepository
                .findByUserUserIdAndMissionDateAndStatus(userId, date, RecommendedMissionStatus.IN_PROGRESS)
                .stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.MISSION_NOT_IN_PROGRESS));
    }
}
