package com.vantageit.road_freight_rate_engine.rateengine.surcharge;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.LineItem;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.SurchargeRate;

/**
 * One surcharge in the stack. Adding a currently-out-of-scope surcharge later means adding one new
 * class implementing this interface — no orchestration logic in {@link
 * SurchargeStackComputationService} needs to change.
 *
 * <p>{@link #surchargeCode()} is an addition beyond the two methods described in the original
 * prompt — the orchestrator needs to know which {@code surcharge_rates} row to resolve for a rule
 * *before* calling {@link #compute}, since {@code compute} takes the already-resolved row as a
 * parameter rather than looking it up itself.
 */
public interface SurchargeRule {

    /** The {@code surcharge_code} this rule looks up in {@code surcharge_rates}. */
    String surchargeCode();

    boolean isApplicable(SurchargeContext context);

    LineItem compute(SurchargeContext context, SurchargeRate rateRow);
}
