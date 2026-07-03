/**
 * Pipeline Stage 4: base freight rate computation. Resolves which {@code RoadFreightRate} row
 * applies (there can legitimately be more than one active row for the same lane/vehicle/load
 * type), then computes the BASE_FREIGHT line item from it.
 */
package com.vantageit.road_freight_rate_engine.rateengine.basefreight;
