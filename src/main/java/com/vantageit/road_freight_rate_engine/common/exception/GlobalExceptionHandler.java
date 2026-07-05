package com.vantageit.road_freight_rate_engine.common.exception;

import com.vantageit.road_freight_rate_engine.common.dto.ErrorResponse;
import com.vantageit.road_freight_rate_engine.items.ItemNotFoundException;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.ValidationErrorResponse;
import com.vantageit.road_freight_rate_engine.rateengine.orchestration.PipelineValidationException;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ItemNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ItemNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // 400, not 422: the request body never successfully became a DTO at all (malformed JSON, an
    // invalid enum literal, a type mismatch), so InputValidationService never ran -- a genuinely
    // different failure category from a well-formed request that fails business validation.
    // Deliberately the generic ErrorResponse shape, not ValidationErrorResponse: the latter's
    // per-field error list assumes a validation pass that never happened here.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedRequest(HttpMessageNotReadableException ex) {
        ErrorResponse error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Malformed request body: " + ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // 422, not 400: matches ValidationErrorResponse's own Javadoc ("response body for the 422
    // validation error contract") and covers both genuine Stage 1 field validation failures and
    // every translated downstream pipeline exception (see PipelineExceptionTranslator) -- both
    // arrive here as the same exception type, already in the API's own error shape.
    @ExceptionHandler(PipelineValidationException.class)
    public ResponseEntity<ValidationErrorResponse> handleRateEngineValidation(PipelineValidationException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(ex.getErrorResponse());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .toList();
        ErrorResponse error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Validation failed", details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        ErrorResponse error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // 415, its natural status -- found via the QA test catalog's "missing Content-Type header"
    // scenario: without this explicit handler, the broad Exception.class catch-all below claims
    // it instead (since @RestControllerAdvice's own handlers take priority over Spring MVC's
    // default exception resolution for any type they declare, and Exception.class matches
    // everything), turning a well-understood, correctly-statused framework exception into a
    // misleading 500.
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        ErrorResponse error = new ErrorResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(error);
    }

    // 405, same reasoning as handleUnsupportedMediaType above -- also found via the QA catalog
    // (its "wrong HTTP method" scenario), same root cause: the broad catch-all below claims this
    // otherwise-correctly-statused Spring MVC exception too, absent an explicit handler.
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        ErrorResponse error = new ErrorResponse(HttpStatus.METHOD_NOT_ALLOWED.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        // Logged, not silently swallowed: this is the catch-all for anything no other handler
        // claimed, so it's the one place a genuinely unexpected server-side failure would
        // otherwise vanish with zero trace beyond the 500 itself.
        log.error("Unhandled exception reaching GlobalExceptionHandler's catch-all", ex);
        ErrorResponse error = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "An unexpected error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
