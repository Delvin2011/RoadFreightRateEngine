package com.vantageit.road_freight_rate_engine.rateengine.api.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** IMDG/UN packing group, indicating degree of danger for hazmat cargo. */
public enum PackingGroup {

    I("i"),
    II("ii"),
    III("iii");

    private final String wireValue;

    PackingGroup(String wireValue) {
        this.wireValue = wireValue;
    }

    @JsonValue
    public String getWireValue() {
        return wireValue;
    }

    @JsonCreator
    public static PackingGroup fromWireValue(String wireValue) {
        for (PackingGroup value : values()) {
            if (value.wireValue.equalsIgnoreCase(wireValue)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown PackingGroup: " + wireValue);
    }
}
