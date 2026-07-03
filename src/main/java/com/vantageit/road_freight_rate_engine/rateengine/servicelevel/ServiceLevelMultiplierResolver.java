package com.vantageit.road_freight_rate_engine.rateengine.servicelevel;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.ServiceLevel;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

/**
 * Service level multipliers are a fixed part of this stage's own logic per the architecture spec
 * — they are NOT in the rate tables (unlike every other priced value in this codebase, which is
 * always DB-sourced). Named constants, looked up via a map rather than an if/else chain.
 */
public final class ServiceLevelMultiplierResolver {

    public static final BigDecimal ECONOMY_MULTIPLIER = new BigDecimal("1.00");
    public static final BigDecimal STANDARD_MULTIPLIER = new BigDecimal("1.15");
    public static final BigDecimal EXPRESS_MULTIPLIER = new BigDecimal("1.40");
    public static final BigDecimal DEDICATED_MULTIPLIER = new BigDecimal("1.65");

    private static final Map<ServiceLevel, BigDecimal> MULTIPLIERS = new EnumMap<>(ServiceLevel.class);

    static {
        MULTIPLIERS.put(ServiceLevel.ECONOMY, ECONOMY_MULTIPLIER);
        MULTIPLIERS.put(ServiceLevel.STANDARD, STANDARD_MULTIPLIER);
        MULTIPLIERS.put(ServiceLevel.EXPRESS, EXPRESS_MULTIPLIER);
        MULTIPLIERS.put(ServiceLevel.DEDICATED, DEDICATED_MULTIPLIER);
    }

    private ServiceLevelMultiplierResolver() {
    }

    public static BigDecimal resolve(ServiceLevel serviceLevel) {
        BigDecimal multiplier = MULTIPLIERS.get(serviceLevel);
        if (multiplier == null) {
            // Unreachable while ServiceLevel only has the 4 values above and MULTIPLIERS covers
            // all of them — defensive guard against the enum growing without this map keeping up.
            throw new IllegalStateException("No multiplier configured for service level: " + serviceLevel);
        }
        return multiplier;
    }
}
