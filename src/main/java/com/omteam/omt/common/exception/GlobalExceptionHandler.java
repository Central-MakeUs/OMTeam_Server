package com.omteam.omt.common.exception;


import com.omteam.omt.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handle(BusinessException e) {
        return ResponseEntity
                .status(e.getStatus())
                .body(ApiResponse.fail(e.getError()));
    }
}
