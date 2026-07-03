package com.vantageit.road_freight_rate_engine.rateengine.api.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CargoClass {

    GENERAL("general"),
    HAZMAT("hazmat"),
    PERISHABLE("perishable"),
    OVERSIZED("oversized"),
    HIGH_VALUE("high_value"),
    LIVE_ANIMALS("live_animals"),
    FRAGILE("fragile");

    private final String wireValue;

    CargoClass(String wireValue) {
        this.wireValue = wireValue;
    }

    @JsonValue
    public String getWireValue() {
        return wireValue;
    }

    @JsonCreator
    public static CargoClass fromWireValue(String wireValue) {
        for (CargoClass value : values()) {
            if (value.wireValue.equalsIgnoreCase(wireValue)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown CargoClass: " + wireValue);
    }
}
