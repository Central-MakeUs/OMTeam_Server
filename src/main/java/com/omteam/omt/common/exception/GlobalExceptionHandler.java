package com.omteam.omt.common.exception;

import com.omteam.omt.common.response.ApiError;
import com.omteam.omt.common.response.ApiResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(new ApiError(errorCode.getCode(), e.getMessage())));
    }

    /**
     * JWT 만료 예외 처리
     */
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ApiResponse<Void>> handleExpiredJwtException(ExpiredJwtException e) {
        ErrorCode errorCode = ErrorCode.EXPIRED_TOKEN;
        log.warn("ExpiredJwtException: {}", e.getMessage());

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(new ApiError(errorCode.getCode(), errorCode.getMessage())));
    }

    /**
     * JWT 검증 실패 예외 처리
     */
    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiResponse<Void>> handleJwtException(JwtException e) {
        ErrorCode errorCode = ErrorCode.INVALID_TOKEN;
        log.warn("JwtException: {}", e.getMessage());

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(new ApiError(errorCode.getCode(), errorCode.getMessage())));
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

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(new ApiError(errorCode.getCode(), message)));
    }

    /**
     * HTTP 메서드 지원하지 않음 예외 처리
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e) {
        ErrorCode errorCode = ErrorCode.METHOD_NOT_ALLOWED;
        log.warn("MethodNotSupportedException: {}", e.getMessage());

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(new ApiError(errorCode.getCode(), errorCode.getMessage())));
    }

    /**
     * IllegalArgumentException 처리 - 잘못된 인자
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
        log.warn("IllegalArgumentException: {}", e.getMessage());

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(new ApiError(errorCode.getCode(), e.getMessage())));
    }

    /**
     * HttpMessageNotReadableException 처리 - JSON 파싱 실패 (Enum 바인딩 실패 포함)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e) {
        ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
        String message = extractReadableMessage(e);
        log.warn("HttpMessageNotReadableException: {}", e.getMessage());

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(new ApiError(errorCode.getCode(), message)));
    }

    private String extractReadableMessage(HttpMessageNotReadableException e) {
        Throwable cause = e.getCause();
        if (cause instanceof com.fasterxml.jackson.databind.exc.InvalidFormatException invalidFormatException) {
            Class<?> targetType = invalidFormatException.getTargetType();
            if (targetType != null && targetType.isEnum()) {
                Object[] enumConstants = targetType.getEnumConstants();
                String allowedValues = java.util.Arrays.stream(enumConstants)
                        .map(Object::toString)
                        .collect(java.util.stream.Collectors.joining(", "));
                return String.format("'%s'은(는) 유효하지 않은 값입니다. 허용된 값: [%s]",
                        invalidFormatException.getValue(), allowedValues);
            }
        }
        return "요청 본문을 읽을 수 없습니다";
    }

    /**
     * 기타 모든 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        log.error("UnhandledException: ", e);

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(new ApiError(errorCode.getCode(), errorCode.getMessage())));
    }
}
