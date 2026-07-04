package com.vantageit.road_freight_rate_engine.rateengine.orchestration;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.ValidationErrorResponse;
import lombok.Getter;

/**
 * Carries a structured {@link ValidationErrorResponse} out of {@link PipelineOrchestrationService},
 * for both genuine Stage 1 validation failures (a {@code List} of real per-field errors) and every
 * translated downstream stage exception (a single {@code _pipeline}-scoped error — see {@link
 * PipelineExceptionTranslator}). One exception type for both cases so a caller has one consistent
 * way to retrieve the structured error response regardless of which stage failed.
 */
@Getter
public class PipelineValidationException extends RuntimeException {

    private final ValidationErrorResponse errorResponse;

    public PipelineValidationException(ValidationErrorResponse errorResponse) {
        super("Rate computation failed: " + errorResponse.errors());
        this.errorResponse = errorResponse;
    }
}
