package com.vantageit.road_freight_rate_engine.common.dto;

import java.time.Instant;
import java.util.List;

public record
ErrorResponse(
        int status,
        String message,
        Instant timestamp,
        List<String> details
) {

    public ErrorResponse(int status, String message) {
        this(status, message, Instant.now(), List.of());
    }

    public ErrorResponse(int status, String message, List<String> details) {
        this(status, message, Instant.now(), details);
    }
}
