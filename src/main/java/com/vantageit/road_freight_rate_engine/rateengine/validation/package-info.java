/**
 * Stage 1 of the rate engine computation pipeline: input validation & normalisation.
 *
 * <p>Pure business-rule validation over the Stage 1 API DTOs ({@code ...rateengine.api.dto}) —
 * no persistence, no database queries, no pricing. Must not depend on
 * {@code ...rateengine.domain.repository}.
 */
package com.vantageit.road_freight_rate_engine.rateengine.validation;
