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

        UserOnboarding onboarding = onboardingRepository.findByUserId(userId)
                .orElse(null);

        UserCharacter character = characterRepository.findById(userId)
                .orElse(null);

        return UserContext.builder()
                .nickname(user.getNickname())
                .appGoal(onboarding != null ? onboarding.getAppGoalText() : null)
                .recentMissionSuccessRate(calculateSuccessRate(userId))
                .currentLevel(character != null ? character.getLevel() : 1)
                .successCount(character != null ? character.getSuccessCount() : 0)
                .preferredExercise(onboarding != null ? onboarding.getPreferredExerciseText() : null)
                .lifestyleType(onboarding != null && onboarding.getLifestyleType() != null
                        ? onboarding.getLifestyleType().name() : null)
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
