/**
 * Pipeline Stage 7: service level multiplier & accessorial charges.
 *
 * <p>{@code TOTAL_SELL = (BASE_FREIGHT + Σ SURCHARGES + Σ LEVIES + Σ CLEARANCES + Σ ACCESSORIALS)
 * × SERVICE_LEVEL_MULTIPLIER × CURRENCY_FACTOR} — this package owns the multiplier and the
 * accessorial line items, which are appended <b>after</b> the multiplier, unmultiplied. Currency
 * conversion and VAT are out of scope here (later stages' job).
 *
 * <p>Built as an isolated, input-agnostic consumer of {@link
 * com.vantageit.road_freight_rate_engine.rateengine.servicelevel.PreMultiplierTotals} — the
 * surcharges (pipeline Stage 5) and clearances (pipeline Stage 6) stages don't exist in this
 * codebase yet, so this package has no dependency on them and never will need to change when
 * they're built; they'll simply supply real sums instead of the zero/placeholder values used today.
 */
package com.vantageit.road_freight_rate_engine.rateengine.servicelevel;
