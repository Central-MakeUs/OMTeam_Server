package com.omteam.omt.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common (C)
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "C002", "요청한 리소스를 찾을 수 없습니다"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C003", "서버 오류가 발생했습니다"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C004", "지원하지 않는 HTTP 메서드입니다"),

    // Auth (A)
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "인증이 필요합니다"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A002", "유효하지 않은 토큰입니다"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "A003", "만료된 토큰입니다"),
    OAUTH_PROVIDER_NOT_FOUND(HttpStatus.BAD_REQUEST, "A004", "지원하지 않는 소셜 로그인 제공자입니다"),
    INVALID_OAUTH_TOKEN(HttpStatus.UNAUTHORIZED, "A005", "유효하지 않은 소셜 로그인 토큰입니다"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "A006", "유효하지 않은 리프레시 토큰입니다"),

    // User (U)
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다"),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "U002", "이미 사용 중인 닉네임입니다"),
    ONBOARDING_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "U003", "온보딩을 완료해주세요"),
    ONBOARDING_ALREADY_COMPLETED(HttpStatus.CONFLICT, "U004", "이미 온보딩이 완료되었습니다"),
    ONBOARDING_NOT_FOUND(HttpStatus.NOT_FOUND, "U005", "온보딩 정보를 찾을 수 없습니다"),

    // Mission (M)
    MISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "미션을 찾을 수 없습니다"),
    DAILY_MISSION_ALREADY_EXISTS(HttpStatus.CONFLICT, "M002", "오늘의 미션이 이미 존재합니다"),
    MISSION_ALREADY_COMPLETED(HttpStatus.CONFLICT, "M003", "이미 완료된 미션입니다"),
    MISSION_NOT_SELECTED(HttpStatus.BAD_REQUEST, "M004", "미션을 먼저 선택해주세요"),
    MISSION_NOT_IN_PROGRESS(HttpStatus.BAD_REQUEST, "M005", "진행 중인 미션이 없습니다"),
    MISSION_ALREADY_IN_PROGRESS(HttpStatus.CONFLICT, "M006", "이미 진행 중인 미션이 있습니다"),
    NO_RECOMMENDED_MISSIONS(HttpStatus.NOT_FOUND, "M007", "추천된 미션이 없습니다"),
    INVALID_MISSION_STATUS(HttpStatus.BAD_REQUEST, "M008", "유효하지 않은 미션 상태입니다"),
    MISSION_RESULT_ALREADY_EXISTS(HttpStatus.CONFLICT, "M009", "오늘의 미션 결과가 이미 존재합니다"),

    // AI Server (AI)
    AI_SERVER_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "AI001", "AI 서버 응답 오류입니다"),
    AI_SERVER_CONNECTION_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "AI002", "AI 서버 연결에 실패했습니다"),

    // Chat (CH)
    INVALID_CHAT_INPUT(HttpStatus.BAD_REQUEST, "CH001", "유효하지 않은 채팅 입력입니다"),
    CHAT_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "CH002", "채팅 세션을 찾을 수 없습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
