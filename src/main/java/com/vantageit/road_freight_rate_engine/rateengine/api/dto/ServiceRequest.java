package com.vantageit.road_freight_rate_engine.rateengine.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

/**
 * @param deliveryDeadline nullable — only set when the customer requires delivery by a specific date
 */
public record ServiceRequest(
        @JsonProperty("service_level") ServiceLevel serviceLevel,
        @JsonProperty("collection_date") LocalDate collectionDate,
        @JsonProperty("delivery_deadline") LocalDate deliveryDeadline,
        @JsonProperty("after_hours_collection") Boolean afterHoursCollection,
        @JsonProperty("after_hours_delivery") Boolean afterHoursDelivery,
        @JsonProperty("tail_lift_required") Boolean tailLiftRequired,
        @JsonProperty("driver_assist_required") Boolean driverAssistRequired,
        @JsonProperty("dedicated_vehicle") Boolean dedicatedVehicle,
        @JsonProperty("security_escort_required") Boolean securityEscortRequired
) {
}
