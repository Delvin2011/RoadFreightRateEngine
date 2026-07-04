/**
 * Pipeline Stage 6: clearance & compliance charges. For cross-border routes, appends border
 * clearance fees; independent of route type, appends compliance documentation fees wherever
 * specific cargo flags require them.
 *
 * <p>Mirrors {@code ...rateengine.surcharge}'s {@code SurchargeRule} shape exactly ({@link
 * com.vantageit.road_freight_rate_engine.rateengine.clearance.ClearanceRule}) for consistency
 * between the two catalogue-driven stages.
 *
 * <p>This package also has out-of-scope items — see {@link
 * com.vantageit.road_freight_rate_engine.rateengine.clearance.ClearanceComputationService}'s
 * Javadoc, which cross-references the existing {@code surcharge_catalogue_deferred_items} project
 * memory rather than duplicating it (three of this stage's gaps were already recorded there).
 */
package com.vantageit.road_freight_rate_engine.rateengine.clearance;
