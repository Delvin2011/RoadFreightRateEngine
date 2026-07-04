package com.vantageit.road_freight_rate_engine.rateengine.clearance;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.LineItem;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.SurchargeRate;

/**
 * One clearance/compliance charge. Mirrors {@code SurchargeRule}'s shape exactly (same three
 * methods) for consistency between the two catalogue-driven stages — adding a currently-out-of-scope
 * charge later means adding one new class, no orchestration changes in {@link
 * ClearanceComputationService}.
 */
public interface ClearanceRule {

    /** The {@code surcharge_code} this rule looks up in {@code surcharge_rates}. */
    String surchargeCode();

    boolean isApplicable(ClearanceContext context);

    LineItem compute(ClearanceContext context, SurchargeRate rateRow);
}
