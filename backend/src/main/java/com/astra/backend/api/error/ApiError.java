package com.astra.backend.api.error;

import java.time.Instant;
import java.util.Map;

public record ApiError(
        String code,
        String message,
        Instant timestamp,
        String path,
        Map<String, String> fieldErrors
) {
    public static ApiError of(String code, String message, String path, Map<String, String> fieldErrors) {
        return new ApiError(code, message, Instant.now(), path, fieldErrors);
    }
}

