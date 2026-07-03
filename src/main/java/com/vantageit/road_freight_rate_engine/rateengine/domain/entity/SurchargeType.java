package com.vantageit.road_freight_rate_engine.rateengine.domain.entity;

public enum SurchargeType {

    PCT_OF_BASE("pct_of_base"),
    FLAT("flat"),
    PER_KM("per_km"),
    PER_HOUR("per_hour"),
    PER_UNIT("per_unit");

    private final String wireValue;

    SurchargeType(String wireValue) {
        this.wireValue = wireValue;
    }

    public String getWireValue() {
        return wireValue;
    }

    public static SurchargeType fromWireValue(String wireValue) {
        for (SurchargeType value : values()) {
            if (value.wireValue.equalsIgnoreCase(wireValue)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown SurchargeType: " + wireValue);
    }
}
