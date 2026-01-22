package com.omteam.omt.common.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.omteam.omt.common.response.ApiError;
import com.omteam.omt.common.response.ApiResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * BusinessException 처리 - 비즈니스 로직 예외
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("BusinessException: {} - {}", errorCode.getCode(), e.getMessage());
        return createErrorResponse(errorCode, e.getMessage());
    }

    /**
     * JWT 만료 예외 처리
     */
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ApiResponse<Void>> handleExpiredJwtException(ExpiredJwtException e) {
        log.warn("ExpiredJwtException: {}", e.getMessage());
        return createErrorResponse(ErrorCode.EXPIRED_TOKEN);
    }

    /**
     * JWT 검증 실패 예외 처리
     */
    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiResponse<Void>> handleJwtException(JwtException e) {
        log.warn("JwtException: {}", e.getMessage());
        return createErrorResponse(ErrorCode.INVALID_TOKEN);
    }

    /**
     * Validation 예외 처리 - @Valid 검증 실패
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse(errorCode.getMessage());

        log.warn("ValidationException: {}", message);
        return createErrorResponse(errorCode, message);
    }

    /**
     * HTTP 메서드 지원하지 않음 예외 처리
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e) {
        log.warn("MethodNotSupportedException: {}", e.getMessage());
        return createErrorResponse(ErrorCode.METHOD_NOT_ALLOWED);
    }

    /**
     * IllegalArgumentException 처리 - 잘못된 인자
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("IllegalArgumentException: {}", e.getMessage());
        return createErrorResponse(ErrorCode.INVALID_INPUT_VALUE, e.getMessage());
    }

    /**
     * HttpMessageNotReadableException 처리 - JSON 파싱 실패 (Enum 바인딩 실패 포함)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e) {
        String message = extractReadableMessage(e);
        log.warn("HttpMessageNotReadableException: {}", e.getMessage());
        return createErrorResponse(ErrorCode.INVALID_INPUT_VALUE, message);
    }

    private String extractReadableMessage(HttpMessageNotReadableException e) {
        Throwable cause = e.getCause();
        if (cause instanceof InvalidFormatException invalidFormatException) {
            Class<?> targetType = invalidFormatException.getTargetType();
            if (targetType != null && targetType.isEnum()) {
                Object[] enumConstants = targetType.getEnumConstants();
                String allowedValues = Arrays.stream(enumConstants)
                        .map(Object::toString)
                        .collect(Collectors.joining(", "));
                return String.format("'%s'은(는) 유효하지 않은 값입니다. 허용된 값: [%s]",
                        invalidFormatException.getValue(), allowedValues);
            }
        }
        return "요청 본문을 읽을 수 없습니다";
    }

    private ResponseEntity<ApiResponse<Void>> createErrorResponse(HttpStatus status, String code, String message) {
        return ResponseEntity
                .status(status)
                .body(ApiResponse.fail(new ApiError(code, message)));
    }

    private ResponseEntity<ApiResponse<Void>> createErrorResponse(ErrorCode errorCode) {
        return createErrorResponse(errorCode.getStatus(), errorCode.getCode(), errorCode.getMessage());
    }

    private ResponseEntity<ApiResponse<Void>> createErrorResponse(ErrorCode errorCode, String message) {
        return createErrorResponse(errorCode.getStatus(), errorCode.getCode(), message);
    }

    /**
     * 기타 모든 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("UnhandledException: ", e);
        return createErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
