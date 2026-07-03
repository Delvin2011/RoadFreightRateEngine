package com.vantageit.road_freight_rate_engine.rateengine.validation;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.ValidationError;
import java.util.List;

/**
 * Outcome of {@link InputValidationService#validate}. {@code errors} are hard failures (the
 * request cannot be priced); {@code flags} are informational (e.g. abnormal-dimension notices)
 * that don't block pricing and can be present regardless of {@link #isValid()}.
 */
public record ValidationResult(List<ValidationError> errors, List<String> flags) {

    public static ValidationResult valid() {
        return new ValidationResult(List.of(), List.of());
    }

    public static ValidationResult valid(List<String> flags) {
        return new ValidationResult(List.of(), flags);
    }

    public static ValidationResult invalid(List<ValidationError> errors) {
        return new ValidationResult(errors, List.of());
    }

    public static ValidationResult invalid(List<ValidationError> errors, List<String> flags) {
        return new ValidationResult(errors, flags);
    }

    public boolean isValid() {
        return errors.isEmpty();
    }
}
