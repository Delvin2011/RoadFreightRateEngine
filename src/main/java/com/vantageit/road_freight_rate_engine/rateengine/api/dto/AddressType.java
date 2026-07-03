package com.vantageit.road_freight_rate_engine.rateengine.api.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AddressType {

    DEPOT("depot"),
    DOOR_TO_DOOR("door_to_door"),
    PORT("port"),
    INLAND_DEPOT("inland_depot");

    private final String wireValue;

    AddressType(String wireValue) {
        this.wireValue = wireValue;
    }

    @JsonValue
    public String getWireValue() {
        return wireValue;
    }

    @JsonCreator
    public static AddressType fromWireValue(String wireValue) {
        for (AddressType value : values()) {
            if (value.wireValue.equalsIgnoreCase(wireValue)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown AddressType: " + wireValue);
    }
}
