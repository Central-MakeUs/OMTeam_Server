package com.omteam.omt.common.ai.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserContext {

    private String nickname;
    private String appGoal;                    // 앱 사용 목적
    private Double recentMissionSuccessRate;   // 최근 7일 미션 성공률 (0.0~1.0)
    private Integer currentLevel;              // 캐릭터 레벨
    private Integer successCount;              // 총 미션 성공 횟수
    private String preferredExercise;          // 선호 운동
    private String lifestyleType;              // 생활 패턴 (Enum name)
}
