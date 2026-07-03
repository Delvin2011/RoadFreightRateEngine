/**
 * Request/response DTOs for the rate engine API boundary ({@code POST /api/v1/rate-engine/compute}).
 *
 * <p>This package is the API boundary layer: it defines the JSON contract exposed to clients and
 * must remain independent of the eventual JPA entity model. Do not add persistence annotations
 * (e.g. {@code @Entity}) or reference entity classes from this package — map between DTOs and
 * entities in a dedicated mapper layer instead.
 */
package com.vantageit.road_freight_rate_engine.rateengine.api.dto;
