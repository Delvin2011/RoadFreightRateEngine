package com.vantageit.road_freight_rate_engine.rateengine.surcharge;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.LineItem;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.SurchargeRate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/** Applies when {@code cargo.live_animals == true}. Flat amount, not a percentage. */
@Component
public class LiveAnimalWelfareRule implements SurchargeRule {

    @Override
    public String surchargeCode() {
        return "LIVE_ANIMAL_WELFARE";
    }

    @Override
    public boolean isApplicable(SurchargeContext context) {
        return Boolean.TRUE.equals(context.request().cargo().liveAnimals());
    }

    @Override
    public LineItem compute(SurchargeContext context, SurchargeRate rateRow) {
        BigDecimal amount = rateRow.getRateValue().setScale(2, RoundingMode.HALF_UP);
        return new LineItem("LIVE_ANIMAL_WELFARE", "Live animal welfare compliance", amount, amount);
    }
}
