package com.omteam.omt.character.service;

import com.omteam.omt.character.client.AiEncouragementClient;
import com.omteam.omt.character.client.dto.AiEncouragementRequest;
import com.omteam.omt.character.client.dto.AiEncouragementResponse;
import com.omteam.omt.character.domain.DailyEncouragementSet;
import com.omteam.omt.character.domain.EncouragementMessage;
import com.omteam.omt.character.repository.DailyEncouragementSetRepository;
import com.omteam.omt.mission.domain.DailyMissionResult;
import com.omteam.omt.mission.repository.DailyMissionResultRepository;
import com.omteam.omt.user.domain.User;
import com.omteam.omt.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EncouragementService {

    private final AiEncouragementClient aiEncouragementClient;
    private final DailyEncouragementSetRepository encouragementSetRepository;
    private final DailyMissionResultRepository missionResultRepository;
    private final UserRepository userRepository;

    /**
     * 모든 활성 사용자에 대해 오늘의 격려 메시지를 생성한다.
     */
    @Transactional
    public int generateDailyEncouragementForAllUsers(LocalDate targetDate) {
        List<User> activeUsers = userRepository.findAll().stream()
                .filter(User::isActive)
                .filter(User::isOnboardingCompleted)
                .toList();

        int successCount = 0;
        for (User user : activeUsers) {
            try {
                generateEncouragementForUser(user, targetDate);
                successCount++;
            } catch (Exception e) {
                log.error("격려 메시지 생성 실패: userId={}", user.getUserId(), e);
            }
        }
        return successCount;
    }

    /**
     * 특정 사용자에 대해 오늘의 격려 메시지를 생성한다.
     */
    @Transactional
    public void generateEncouragementForUser(User user, LocalDate targetDate) {
        if (encouragementSetRepository.existsByUserUserIdAndTargetDate(user.getUserId(), targetDate)) {
            log.debug("이미 격려 메시지가 존재함: userId={}, targetDate={}", user.getUserId(), targetDate);
            return;
        }

        LocalDate yesterday = targetDate.minusDays(1);
        DailyMissionResult yesterdayResult = missionResultRepository
                .findByUserUserIdAndMissionDate(user.getUserId(), yesterday)
                .orElse(null);

        AiEncouragementRequest request = buildRequest(user.getUserId(), targetDate, yesterdayResult);
        AiEncouragementResponse response = aiEncouragementClient.requestDailyEncouragement(request);

        DailyEncouragementSet encouragementSet = buildEncouragementSet(user, targetDate, response);
        encouragementSetRepository.save(encouragementSet);

        log.info("격려 메시지 생성 완료: userId={}, targetDate={}", user.getUserId(), targetDate);
    }

    private AiEncouragementRequest buildRequest(Long userId, LocalDate targetDate, DailyMissionResult yesterdayResult) {
        AiEncouragementRequest.AiEncouragementRequestBuilder builder = AiEncouragementRequest.builder()
                .userId(userId)
                .targetDate(targetDate.toString());

        if (yesterdayResult != null) {
            builder.todayMission(AiEncouragementRequest.TodayMission.builder()
                    .missionType(yesterdayResult.getMission().getType())
                    .difficulty(yesterdayResult.getMission().getDifficulty())
                    .result(yesterdayResult.getResult())
                    .failureReason(yesterdayResult.getFailureReason())
                    .build());
        }

        return builder.build();
    }

    private DailyEncouragementSet buildEncouragementSet(User user, LocalDate targetDate, AiEncouragementResponse response) {
        return DailyEncouragementSet.builder()
                .user(user)
                .targetDate(targetDate)
                .praise(toEncouragementMessage(response.getPraise()))
                .retry(toEncouragementMessage(response.getRetry()))
                .normal(toEncouragementMessage(response.getNormal()))
                .push(toEncouragementMessage(response.getPush()))
                .build();
    }

    private EncouragementMessage toEncouragementMessage(AiEncouragementResponse.MessageContent content) {
        if (content == null) {
            return null;
        }
        return EncouragementMessage.builder()
                .title(content.getTitle())
                .message(content.getMessage())
                .build();
    }
}
