/**
 * Pipeline Stage 8 (final stage): currency conversion & VAT.
 *
 * <p>Takes every monetary line item produced by earlier stages, converts non-ZAR amounts to ZAR,
 * sums to a subtotal, applies VAT, and produces the final {@link
 * com.vantageit.road_freight_rate_engine.rateengine.api.dto.Totals}.
 *
 * <p>Pure consumer of {@link
 * com.vantageit.road_freight_rate_engine.rateengine.currencyconversion.MonetaryLineItem} — this
 * stage doesn't know or care which pipeline stage produced each line item, same decoupling
 * discipline as Stage 7's {@code PreMultiplierTotals}.
 */
package com.vantageit.road_freight_rate_engine.rateengine.currencyconversion;
