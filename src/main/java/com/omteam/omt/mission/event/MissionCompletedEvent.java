package com.omteam.omt.mission.event;

import com.omteam.omt.user.domain.User;
import java.time.LocalDate;

/**
 * 미션 완료 시 발행되는 이벤트.
 * (임시) 데일리 분석 결과 생성 트리거 용도 - 분석 조회 기준 확정 후 제거 예정
 */
public record MissionCompletedEvent(User user, LocalDate missionDate) {
}
