/**
 * Pipeline orchestration: wires every pipeline stage together in sequence, producing the final
 * {@link com.vantageit.road_freight_rate_engine.rateengine.api.dto.RateComputeResponse}, with
 * all-or-nothing failure behavior.
 *
 * <p>{@link com.vantageit.road_freight_rate_engine.rateengine.orchestration.PipelineOrchestrationService}
 * wires and translates; it contains no pricing/business logic of its own — every computation
 * belongs to, and stays in, the individual stage service that owns it.
 *
 * <p><b>Known, tracked limitation, not fixed here</b> (see the {@code
 * catalogue_currency_consistency_followup} project memory): Stages 9 (surcharges) and 10
 * (clearances) sum their own line items as raw totals with no currency-consistency guard, unlike
 * Stage 7's accessorials. This package works around it pragmatically — see {@link
 * com.vantageit.road_freight_rate_engine.rateengine.orchestration.PipelineOrchestrationService}'s
 * Javadoc for exactly how — rather than fixing either stage's internals, which was explicitly out
 * of scope for this work.
 */
package com.vantageit.road_freight_rate_engine.rateengine.orchestration;
