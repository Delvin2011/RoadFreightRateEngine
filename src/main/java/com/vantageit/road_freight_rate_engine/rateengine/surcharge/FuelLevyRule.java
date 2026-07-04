package com.vantageit.road_freight_rate_engine.rateengine.surcharge;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.LineItem;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.SurchargeRate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/** Always applies. {@code rate_value} is a percentage of BASE_FREIGHT. */
@Component
public class FuelLevyRule implements SurchargeRule {

    @Override
    public String surchargeCode() {
        return "FUEL_LEVY";
    }

    @Override
    public boolean isApplicable(SurchargeContext context) {
        return true;
    }

    @Override
    public LineItem compute(SurchargeContext context, SurchargeRate rateRow) {
        BigDecimal amount = context.baseFreightResult().baseFreightAmount()
                .multiply(rateRow.getRateValue())
                .setScale(2, RoundingMode.HALF_UP);
        return new LineItem("FUEL_LEVY", "Fuel levy", amount, amount);
    }
}
