package com.omteam.omt.common.response;

public record ApiResponse<T>(boolean success, T data, ApiError error) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> fail(ApiError error) {
        return new ApiResponse<>(false, null, error);
    }
}
