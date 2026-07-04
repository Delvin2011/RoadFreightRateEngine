/**
 * Pipeline Stage 5: surcharge stack computation. Computes every applicable surcharge as its own
 * line item — percentage surcharges apply to BASE_FREIGHT (Stage 6's output) specifically, never
 * to a running total that includes other surcharges, to prevent compounding. One documented
 * exception: {@link com.vantageit.road_freight_rate_engine.rateengine.surcharge.FrozenGoodsUpliftRule}
 * applies to {@code REEFER_RUNNING}'s amount instead.
 *
 * <p>This package does not implement the full architecture-spec surcharge catalogue — see {@link
 * com.vantageit.road_freight_rate_engine.rateengine.surcharge.SurchargeStackComputationService}'s
 * Javadoc for the complete list of out-of-scope surcharges and why, and the {@code
 * surcharge_catalogue_deferred_items} project memory for the consolidated record.
 */
package com.vantageit.road_freight_rate_engine.rateengine.surcharge;
