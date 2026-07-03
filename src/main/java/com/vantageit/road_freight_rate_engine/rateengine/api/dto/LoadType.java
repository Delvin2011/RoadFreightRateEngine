package com.vantageit.road_freight_rate_engine.rateengine.api.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum LoadType {

    FTL("ftl"),
    LTL("ltl"),
    FLATBED("flatbed"),
    REEFER("reefer"),
    TANKER("tanker"),
    LOWBED("lowbed"),
    SIDE_TIPPER("side_tipper"),
    BULK_TIPPER("bulk_tipper");

    private final String wireValue;

    LoadType(String wireValue) {
        this.wireValue = wireValue;
    }

    @JsonValue
    public String getWireValue() {
        return wireValue;
    }

    @JsonCreator
    public static LoadType fromWireValue(String wireValue) {
        for (LoadType value : values()) {
            if (value.wireValue.equalsIgnoreCase(wireValue)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown LoadType: " + wireValue);
    }
}
