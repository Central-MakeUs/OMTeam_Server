package com.omteam.omt.common.ai.service;

import com.omteam.omt.common.ai.dto.UserContext;
import com.omteam.omt.common.exception.BusinessException;
import com.omteam.omt.common.exception.ErrorCode;
import com.omteam.omt.mission.domain.DailyMissionResult;
import com.omteam.omt.mission.domain.MissionResult;
import com.omteam.omt.mission.repository.DailyMissionResultRepository;
import com.omteam.omt.user.domain.User;
import com.omteam.omt.user.domain.UserCharacter;
import com.omteam.omt.user.domain.UserOnboarding;
import com.omteam.omt.user.repository.UserCharacterRepository;
import com.omteam.omt.user.repository.UserOnboardingRepository;
import com.omteam.omt.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserContextService {

    private final UserRepository userRepository;
    private final UserOnboardingRepository onboardingRepository;
    private final UserCharacterRepository characterRepository;
    private final DailyMissionResultRepository missionResultRepository;

    private static final int RECENT_DAYS = 7;

    public UserContext buildContext(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Optional<UserOnboarding> onboardingOpt = onboardingRepository.findByUserId(userId);
        Optional<UserCharacter> characterOpt = characterRepository.findById(userId);

        return UserContext.builder()
                .nickname(user.getNickname())
                .appGoal(onboardingOpt.map(UserOnboarding::getAppGoalText).orElse(null))
                .recentMissionSuccessRate(calculateSuccessRate(userId))
                .currentLevel(characterOpt.map(UserCharacter::getLevel).orElse(1))
                .successCount(characterOpt.map(UserCharacter::getSuccessCount).orElse(0))
                .preferredExercise(onboardingOpt.map(UserOnboarding::getPreferredExerciseText).orElse(null))
                .lifestyleType(onboardingOpt.map(UserOnboarding::getLifestyleType).map(Enum::name).orElse(null))
                .build();
    }

    private Double calculateSuccessRate(Long userId) {
        LocalDate startDate = LocalDate.now().minusDays(RECENT_DAYS);
        LocalDate endDate = LocalDate.now().minusDays(1);  // 어제까지

        List<DailyMissionResult> recentResults = missionResultRepository
                .findByUserUserIdAndMissionDateBetween(userId, startDate, endDate);

        if (recentResults.isEmpty()) {
            return null;
        }

        long successCount = recentResults.stream()
                .filter(r -> r.getResult() == MissionResult.SUCCESS)
                .count();

        return (double) successCount / recentResults.size();
    }
}
