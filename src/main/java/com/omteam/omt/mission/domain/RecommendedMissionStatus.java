package com.omteam.omt.mission.domain;

public enum RecommendedMissionStatus {
    RECOMMENDED,    // 추천됨 (아직 선택/시작 안함)
    SELECTED,       // 선택됨 (시작 전)
    IN_PROGRESS,    // 진행 중
    COMPLETED,      // 완료됨 (결과 기록됨)
    EXPIRED         // 만료됨 (재추천으로 비활성화)
}
