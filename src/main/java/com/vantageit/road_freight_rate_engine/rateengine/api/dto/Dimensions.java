package com.vantageit.road_freight_rate_engine.rateengine.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;

/**
 * Cargo dimensions in metres. Serialized as a JSON array {@code [length, width, height]},
 * matching the {@code dimensions_lxwxh_m} field name on the wire.
 */
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
public record Dimensions(
        BigDecimal length,
        BigDecimal width,
        BigDecimal height
) {
}
