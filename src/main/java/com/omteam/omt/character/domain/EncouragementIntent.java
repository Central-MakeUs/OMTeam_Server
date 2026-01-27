package com.omteam.omt.character.domain;

/**
 * 격려 메시지 의도 유형
 */
public enum EncouragementIntent {
    /**
     * 칭찬 - 미션을 잘 수행하고 있는 경우
     */
    PRAISE,

    /**
     * 재시도 권유 - 최근 실패했지만 다시 시도를 권유
     */
    RETRY,

    /**
     * 일반 - 보통 상태의 격려
     */
    NORMAL,

    /**
     * 독려 - 미션을 시작하지 않은 경우 시작 독려
     */
    PUSH
}
