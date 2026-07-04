package com.vantageit.road_freight_rate_engine.rateengine.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

/**
 * @param deliveryDeadline nullable — only set when the customer requires delivery by a specific date
 * @param tailLiftCollection      replaces the old undifferentiated {@code tail_lift_required} —
 *                                see the (now-closed) {@code accessorial_collection_delivery_ambiguity_deferred}
 *                                project memory
 * @param driverAssistLoading     replaces the old undifferentiated {@code driver_assist_required},
 *                                same reasoning as {@code tailLiftCollection}/{@code tailLiftDelivery}
 */
public record ServiceRequest(
        @JsonProperty("service_level") ServiceLevel serviceLevel,
        @JsonProperty("collection_date") LocalDate collectionDate,
        @JsonProperty("delivery_deadline") LocalDate deliveryDeadline,
        @JsonProperty("after_hours_collection") Boolean afterHoursCollection,
        @JsonProperty("after_hours_delivery") Boolean afterHoursDelivery,
        @JsonProperty("tail_lift_collection") Boolean tailLiftCollection,
        @JsonProperty("tail_lift_delivery") Boolean tailLiftDelivery,
        @JsonProperty("driver_assist_loading") Boolean driverAssistLoading,
        @JsonProperty("driver_assist_offloading") Boolean driverAssistOffloading,
        @JsonProperty("dedicated_vehicle") Boolean dedicatedVehicle,
        @JsonProperty("security_escort_required") Boolean securityEscortRequired
) {
}
