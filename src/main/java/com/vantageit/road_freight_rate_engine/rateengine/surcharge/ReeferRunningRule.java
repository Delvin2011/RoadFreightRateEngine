package com.vantageit.road_freight_rate_engine.rateengine.surcharge;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.CargoRequest;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.LineItem;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.LoadType;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.SurchargeRate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/**
 * Applies when {@code cargo.load_type == REEFER} OR {@code cargo.temperature_range_c} is set (an
 * OR, not an AND — either condition alone triggers it). {@code distance_km × rate_per_km}.
 */
@Component
public class ReeferRunningRule implements SurchargeRule {

    @Override
    public String surchargeCode() {
        return "REEFER_RUNNING";
    }

    @Override
    public boolean isApplicable(SurchargeContext context) {
        CargoRequest cargo = context.request().cargo();
        return cargo.loadType() == LoadType.REEFER || cargo.temperatureRangeC() != null;
    }

    @Override
    public LineItem compute(SurchargeContext context, SurchargeRate rateRow) {
        BigDecimal amount = context.lane().distanceKm()
                .multiply(rateRow.getRateValue())
                .setScale(2, RoundingMode.HALF_UP);
        return new LineItem("REEFER_RUNNING", "Reefer running", amount, amount);
    }
}
