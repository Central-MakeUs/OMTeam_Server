package com.omteam.omt.notification.service;

import com.omteam.omt.mission.domain.RecommendedMissionStatus;
import com.omteam.omt.mission.repository.DailyRecommendedMissionRepository;
import com.omteam.omt.notification.constant.NotificationMessages;
import com.omteam.omt.report.service.DailyAnalysisService;
import com.omteam.omt.user.domain.User;
import com.omteam.omt.user.domain.UserNotificationSetting;
import com.omteam.omt.user.domain.UserOnboarding;
import com.omteam.omt.user.repository.UserNotificationSettingRepository;
import com.omteam.omt.user.repository.UserOnboardingRepository;
import com.omteam.omt.user.service.UserQueryService;
import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final UserQueryService userQueryService;
    private final UserOnboardingRepository userOnboardingRepository;
    private final UserNotificationSettingRepository notificationSettingRepository;
    private final DailyRecommendedMissionRepository recommendedMissionRepository;
    private final DailyAnalysisService dailyAnalysisService;
    private final FcmService fcmService;

    @Value("${notification.default.wake-up-time}")
    private String defaultWakeUpTimeStr;

    @Value("${notification.default.bed-time}")
    private String defaultBedTimeStr;

    private LocalTime defaultWakeUpTime;
    private LocalTime defaultBedTime;

    @PostConstruct
    void initDefaultTimes() {
        defaultWakeUpTime = LocalTime.parse(defaultWakeUpTimeStr);
        defaultBedTime = LocalTime.parse(defaultBedTimeStr);
    }

    @Transactional
    public void registerFcmToken(Long userId, String fcmToken) {
        User user = userQueryService.getUser(userId);
        user.updateFcmToken(fcmToken);
    }

    @Transactional
    public void deleteFcmToken(Long userId) {
        User user = userQueryService.getUser(userId);
        user.updateFcmToken(null);
    }

    public void sendRemindNotifications(LocalTime now, LocalDate today) {
        List<UserOnboarding> targets = userOnboardingRepository.findByAvailableStartTimeForNotification(now);
        if (targets.isEmpty()) {
            return;
        }

        List<Long> userIds = extractUserIds(targets);
        Map<Long, UserNotificationSetting> settingMap = buildSettingMap(userIds);

        List<RecommendedMissionStatus> activeStatuses = List.of(
                RecommendedMissionStatus.IN_PROGRESS,
                RecommendedMissionStatus.COMPLETED
        );
        Set<Long> usersWithActiveMissions = new HashSet<>(
                recommendedMissionRepository.findUserIdsHavingActiveMissions(userIds, today, activeStatuses));

        int sentCount = 0;
        for (UserOnboarding onboarding : targets) {
            User user = onboarding.getUser();
            Long userId = user.getUserId();

            UserNotificationSetting setting = settingMap.get(userId);
            if (setting == null || !setting.isRemindEnabled()) {
                continue;
            }
            if (user.getFcmToken() == null || user.getFcmToken().isBlank()) {
                continue;
            }
            if (usersWithActiveMissions.contains(userId)) {
                continue;
            }

            try {
                fcmService.sendNotification(user.getFcmToken(),
                        NotificationMessages.REMIND_TITLE,
                        NotificationMessages.REMIND_BODY);
                sentCount++;
            } catch (Exception e) {
                log.warn("리마인드 알림 발송 실패: userId={}", userId, e);
            }
        }
        log.info("리마인드 알림 발송 완료: total={}, sent={}", targets.size(), sentCount);
    }

    public void sendCheckinNotifications(LocalTime now) {
        List<UserOnboarding> targets = userOnboardingRepository.findByEffectiveWakeUpTime(now, defaultWakeUpTime);
        if (targets.isEmpty()) {
            return;
        }

        List<Long> userIds = extractUserIds(targets);
        Map<Long, UserNotificationSetting> settingMap = buildSettingMap(userIds);

        int sentCount = 0;
        for (UserOnboarding onboarding : targets) {
            User user = onboarding.getUser();
            Long userId = user.getUserId();

            UserNotificationSetting setting = settingMap.get(userId);
            if (setting == null || !setting.isCheckinEnabled()) {
                continue;
            }
            if (user.getFcmToken() == null || user.getFcmToken().isBlank()) {
                continue;
            }

            try {
                fcmService.sendNotification(user.getFcmToken(),
                        NotificationMessages.CHECKIN_TITLE,
                        NotificationMessages.CHECKIN_BODY);
                sentCount++;
            } catch (Exception e) {
                log.warn("체크인 알림 발송 실패: userId={}", userId, e);
            }
        }
        log.info("체크인 알림 발송 완료: total={}, sent={}", targets.size(), sentCount);
    }

    public void sendReviewNotifications(LocalTime now, LocalDate today) {
        LocalTime defaultReviewTime = defaultBedTime.minusHours(1);
        LocalTime targetBedTime = now.plusHours(1);

        List<UserOnboarding> targets = userOnboardingRepository.findByBedTimeForReview(
                targetBedTime, now, defaultReviewTime);
        if (targets.isEmpty()) {
            return;
        }

        List<Long> userIds = extractUserIds(targets);
        Map<Long, UserNotificationSetting> settingMap = buildSettingMap(userIds);
        int sentCount = 0;
        for (UserOnboarding onboarding : targets) {
            User user = onboarding.getUser();
            Long userId = user.getUserId();

            UserNotificationSetting setting = settingMap.get(userId);
            if (setting == null || !setting.isReviewEnabled()) {
                continue;
            }
            if (user.getFcmToken() == null || user.getFcmToken().isBlank()) {
                continue;
            }

            try {
                dailyAnalysisService.generateDailyAnalysisForUser(user, today);
                fcmService.sendNotification(user.getFcmToken(),
                        NotificationMessages.REVIEW_TITLE,
                        NotificationMessages.REVIEW_BODY);
                sentCount++;
            } catch (Exception e) {
                log.warn("회고 알림 발송 실패: userId={}", userId, e);
            }
        }
        log.info("회고 알림 발송 완료: total={}, sent={}", targets.size(), sentCount);
    }

    private List<Long> extractUserIds(List<UserOnboarding> onboardings) {
        return onboardings.stream()
                .map(uo -> uo.getUser().getUserId())
                .collect(Collectors.toList());
    }

    private Map<Long, UserNotificationSetting> buildSettingMap(List<Long> userIds) {
        return notificationSettingRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserNotificationSetting::getUserId, s -> s));
    }
}
