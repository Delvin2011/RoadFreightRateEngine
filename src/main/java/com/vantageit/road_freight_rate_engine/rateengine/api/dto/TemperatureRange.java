package com.vantageit.road_freight_rate_engine.rateengine.api.dto;

import java.math.BigDecimal;

/** Required temperature range in degrees Celsius, for reefer/perishable cargo. */
public record TemperatureRange(
        BigDecimal min,
        BigDecimal max
) {
}
