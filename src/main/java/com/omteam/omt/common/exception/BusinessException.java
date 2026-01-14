package com.omteam.omt.common.exception;

import com.omteam.omt.common.response.ApiError;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {

    private final ApiError error;
    private final HttpStatus status;

    public BusinessException(ApiError error, HttpStatus status) {
        this.error = error;
        this.status = status;
    }
}

