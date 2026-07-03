package com.vantageit.road_freight_rate_engine.rateengine.api.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ServiceLevel {

    ECONOMY("economy"),
    STANDARD("standard"),
    EXPRESS("express"),
    DEDICATED("dedicated");

    private final String wireValue;

    ServiceLevel(String wireValue) {
        this.wireValue = wireValue;
    }

    @JsonValue
    public String getWireValue() {
        return wireValue;
    }

    @JsonCreator
    public static ServiceLevel fromWireValue(String wireValue) {
        for (ServiceLevel value : values()) {
            if (value.wireValue.equalsIgnoreCase(wireValue)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown ServiceLevel: " + wireValue);
    }
}
