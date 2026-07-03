package com.vantageit.road_freight_rate_engine.rateengine.api.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PackageType {

    PALLETISED("palletised"),
    LOOSE("loose"),
    BULK("bulk"),
    DRUMMED("drummed"),
    IBC("ibc"),
    CRATES("crates"),
    ROLLS("rolls");

    private final String wireValue;

    PackageType(String wireValue) {
        this.wireValue = wireValue;
    }

    @JsonValue
    public String getWireValue() {
        return wireValue;
    }

    @JsonCreator
    public static PackageType fromWireValue(String wireValue) {
        for (PackageType value : values()) {
            if (value.wireValue.equalsIgnoreCase(wireValue)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown PackageType: " + wireValue);
    }
}
