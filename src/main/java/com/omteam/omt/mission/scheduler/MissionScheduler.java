package com.omteam.omt.mission.scheduler;

import com.omteam.omt.mission.service.MissionService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MissionScheduler {

    private final MissionService missionService;

    @Scheduled(cron = "0 0 0 * * *")
    public void expireUncompletedMissions() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("미션 만료 스케줄러 시작: targetDate={}", yesterday);

        try {
            int expiredCount = missionService.expireUncompletedMissions(yesterday);
            log.info("미션 만료 스케줄러 완료: expiredCount={}", expiredCount);
        } catch (Exception e) {
            log.error("미션 만료 스케줄러 오류", e);
        }
    }
}
