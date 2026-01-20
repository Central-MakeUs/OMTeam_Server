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

    // Mission (M)
    MISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "미션을 찾을 수 없습니다"),
    DAILY_MISSION_ALREADY_EXISTS(HttpStatus.CONFLICT, "M002", "오늘의 미션이 이미 존재합니다"),
    MISSION_ALREADY_COMPLETED(HttpStatus.CONFLICT, "M003", "이미 완료된 미션입니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
