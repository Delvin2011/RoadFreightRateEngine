/**
 * Pipeline Stage 3: vehicle type selection. Takes the validated request (Stage 3 already ran)
 * plus the resolved lane (Stage 4 already ran) and selects the vehicle category — the eligible
 * set by load type/capacity/zone restriction, then either the cheapest or, for a dedicated
 * vehicle, the smallest viable one, followed by a vehicle-aware overweight sanity check.
 */
package com.vantageit.road_freight_rate_engine.rateengine.vehicleselection;
