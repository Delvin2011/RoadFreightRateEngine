package com.vantageit.road_freight_rate_engine.rateengine.surcharge;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.LineItem;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.SurchargeRate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/**
 * Same trigger as {@link LiveAnimalWelfareRule} ({@code cargo.live_animals == true}) but a
 * distinct flat line item — the two apply together, not as alternatives to each other.
 */
@Component
public class LivestockVehicleCertRule implements SurchargeRule {

    @Override
    public String surchargeCode() {
        return "LIVESTOCK_VEHICLE_CERT";
    }

    @Override
    public boolean isApplicable(SurchargeContext context) {
        return Boolean.TRUE.equals(context.request().cargo().liveAnimals());
    }

    @Override
    public LineItem compute(SurchargeContext context, SurchargeRate rateRow) {
        BigDecimal amount = rateRow.getRateValue().setScale(2, RoundingMode.HALF_UP);
        return new LineItem("LIVESTOCK_VEHICLE_CERT", "Livestock vehicle certification", amount, amount);
    }
}
