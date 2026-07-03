package com.vantageit.road_freight_rate_engine.rateengine.api.dto;

/**
 * @param field   dot-path of the offending request field, e.g. {@code "cargo.hazmat_un_number"}
 * @param code    machine-readable error code, for client-side handling without string-matching {@code message}
 */
public record ValidationError(
        String field,
        String code,
        String message
) {
}
