package com.omteam.omt.chat.domain;

/**
 * 챗봇 액션 타입.
 * AI 호출 없이 서버에서 직접 처리하는 채팅 액션을 구분한다.
 */
public enum ChatActionType {
    COMPLETE_MISSION,        // 미션 결과 등록 (시작/성공/실패 선택)
    MISSION_FAILURE_REASON,  // 실패 사유 입력 후 미션 실패 등록
    NAVIGATE_HOME            // 클라이언트 홈 화면 이동 트리거
}
