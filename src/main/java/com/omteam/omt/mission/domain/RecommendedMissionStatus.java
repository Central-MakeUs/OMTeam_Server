package com.omteam.omt.mission.domain;

public enum RecommendedMissionStatus {
    RECOMMENDED,    // 추천됨 (아직 시작 안함)
    IN_PROGRESS,    // 진행 중
    COMPLETED,      // 완료됨 (결과 기록됨)
    EXPIRED         // 만료됨 (포기 또는 재추천으로 비활성화)
}
