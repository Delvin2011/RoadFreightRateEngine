package com.vantageit.road_freight_rate_engine.rateengine.domain.entity;

/**
 * Which route types a surcharge applies to. Deliberately separate from Stage 1's
 * {@code api.dto.RouteType} (which only has DOMESTIC/CROSS_BORDER) since a surcharge can apply to
 * {@link #BOTH}.
 */
public enum RouteApplicability {

    DOMESTIC("domestic"),
    CROSS_BORDER("cross_border"),
    BOTH("both");

    private final String wireValue;

    RouteApplicability(String wireValue) {
        this.wireValue = wireValue;
    }

    public String getWireValue() {
        return wireValue;
    }

    public static RouteApplicability fromWireValue(String wireValue) {
        for (RouteApplicability value : values()) {
            if (value.wireValue.equalsIgnoreCase(wireValue)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown RouteApplicability: " + wireValue);
    }
}
