package com.vantageit.road_freight_rate_engine.rateengine.domain.entity;

public enum RateBasis {

    PER_KM("per_km"),
    PER_TON("per_ton"),
    FLAT("flat"),
    PER_PALLET("per_pallet"),
    PER_CBM("per_cbm");

    private final String wireValue;

    RateBasis(String wireValue) {
        this.wireValue = wireValue;
    }

    public String getWireValue() {
        return wireValue;
    }

    public static RateBasis fromWireValue(String wireValue) {
        for (RateBasis value : values()) {
            if (value.wireValue.equalsIgnoreCase(wireValue)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown RateBasis: " + wireValue);
    }
}
