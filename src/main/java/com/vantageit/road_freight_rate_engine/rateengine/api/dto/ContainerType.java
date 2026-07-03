package com.vantageit.road_freight_rate_engine.rateengine.api.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Java enum constants can't start with a digit, so wire values (e.g. {@code "20ft_std"}) are
 * mapped explicitly rather than derived from the constant name.
 */
public enum ContainerType {

    TWENTY_FT_STD("20ft_std"),
    FORTY_FT_STD("40ft_std"),
    FORTY_FT_HC("40ft_hc"),
    TWENTY_FT_REEFER("20ft_reefer"),
    FORTY_FT_REEFER("40ft_reefer");

    private final String wireValue;

    ContainerType(String wireValue) {
        this.wireValue = wireValue;
    }

    @JsonValue
    public String getWireValue() {
        return wireValue;
    }

    @JsonCreator
    public static ContainerType fromWireValue(String wireValue) {
        for (ContainerType value : values()) {
            if (value.wireValue.equalsIgnoreCase(wireValue)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown ContainerType: " + wireValue);
    }
}
