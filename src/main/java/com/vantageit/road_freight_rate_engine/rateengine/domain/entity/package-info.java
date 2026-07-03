/**
 * JPA entities for the rate engine domain (zones, vehicle categories, road freight rates,
 * surcharge rates).
 *
 * <p>This is a separate model from {@code ...rateengine.api.dto} and must not be reused as, or
 * merged with, those API DTOs — map between the two in a dedicated mapper layer.
 *
 * <p>Rate rows ({@link com.vantageit.road_freight_rate_engine.rateengine.domain.entity.RoadFreightRate}
 * and {@link com.vantageit.road_freight_rate_engine.rateengine.domain.entity.SurchargeRate}) are
 * immutable once activated: a rate change is always a new row, never an update to priced fields
 * on an existing one.
 */
package com.vantageit.road_freight_rate_engine.rateengine.domain.entity;
