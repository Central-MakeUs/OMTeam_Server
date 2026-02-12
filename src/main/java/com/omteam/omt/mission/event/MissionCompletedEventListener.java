package com.omteam.omt.mission.event;

import com.omteam.omt.report.service.DailyAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * (임시) 미션 완료 후 데일리 분석 결과를 생성하는 이벤트 리스너.
 * 분석 조회 기준 확정 후 제거 예정
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MissionCompletedEventListener {

    private final DailyAnalysisService dailyAnalysisService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMissionCompleted(MissionCompletedEvent event) {
        try {
            dailyAnalysisService.generateDailyAnalysisForUser(event.user(), event.missionDate());
        } catch (Exception e) {
            log.warn("데일리 분석 결과 생성 실패 (미션 완료에는 영향 없음): userId={}, error={}",
                    event.user().getUserId(), e.getMessage());
        }
    }
}
