package com.vantageit.road_freight_rate_engine.rateengine.api.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RouteType {

    DOMESTIC("domestic"),
    CROSS_BORDER("cross_border");

    private final String wireValue;

    RouteType(String wireValue) {
        this.wireValue = wireValue;
    }

    @JsonValue
    public String getWireValue() {
        return wireValue;
    }

    @JsonCreator
    public static RouteType fromWireValue(String wireValue) {
        for (RouteType value : values()) {
            if (value.wireValue.equalsIgnoreCase(wireValue)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown RouteType: " + wireValue);
    }
}
