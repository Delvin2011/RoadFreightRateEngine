package com.vantageit.road_freight_rate_engine.rateengine.domain.entity;

/**
 * Only {@code METRO_ONLY} exists today. Wire value is kept uppercase (unlike {@link RateBasis}
 * etc.'s lowercase snake_case) to match what {@code V10__seed_vehicle_selection_fixtures.sql}
 * already persisted — changing casing now would need a data migration for no real benefit.
 */
public enum ZoneRestriction {

    METRO_ONLY("METRO_ONLY");

    private final String wireValue;

    ZoneRestriction(String wireValue) {
        this.wireValue = wireValue;
    }

    public String getWireValue() {
        return wireValue;
    }

    public static ZoneRestriction fromWireValue(String wireValue) {
        for (ZoneRestriction value : values()) {
            if (value.wireValue.equalsIgnoreCase(wireValue)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown ZoneRestriction: " + wireValue);
    }
}
