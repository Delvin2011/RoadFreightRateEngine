package com.vantageit.road_freight_rate_engine.rateengine.surcharge;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.LineItem;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.SurchargeRate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/**
 * Applies when {@code cargo.high_value_declared == true}. {@code rate_value} is a percentage of
 * {@code cargo.declared_value_zar} — deliberately not BASE_FREIGHT, unlike most other percentage
 * rules in this stack.
 */
@Component
public class HighValueInsuranceLevyRule implements SurchargeRule {

    @Override
    public String surchargeCode() {
        return "HIGH_VALUE_INSURANCE_LEVY";
    }

    @Override
    public boolean isApplicable(SurchargeContext context) {
        return Boolean.TRUE.equals(context.request().cargo().highValueDeclared());
    }

    @Override
    public LineItem compute(SurchargeContext context, SurchargeRate rateRow) {
        BigDecimal amount = context.request().cargo().declaredValueZar()
                .multiply(rateRow.getRateValue())
                .setScale(2, RoundingMode.HALF_UP);
        return new LineItem("HIGH_VALUE_INSURANCE_LEVY", "High-value cargo insurance levy", amount, amount);
    }
}
