package com.vantageit.road_freight_rate_engine.rateengine.api.dto;

import java.util.List;

/** Response body for the 422 validation error contract. */
public record ValidationErrorResponse(
        String status,
        List<ValidationError> errors
) {
}
