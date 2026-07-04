package com.vantageit.road_freight_rate_engine.rateengine.surcharge;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.CargoClass;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.CargoRequest;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.LineItem;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.PackageType;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.SurchargeRate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/**
 * Applies when {@code cargo.cargo_class == FRAGILE} OR {@code cargo.package_type} in
 * {@code (CRATES, ROLLS)} — an OR, not an AND. Single line item regardless of which condition (or
 * both) triggered it, no double-charging concern since this rule only ever produces one output.
 */
@Component
public class FragileHandlingRule implements SurchargeRule {

    @Override
    public String surchargeCode() {
        return "FRAGILE_HANDLING";
    }

    @Override
    public boolean isApplicable(SurchargeContext context) {
        CargoRequest cargo = context.request().cargo();
        return cargo.cargoClass() == CargoClass.FRAGILE
                || cargo.packageType() == PackageType.CRATES
                || cargo.packageType() == PackageType.ROLLS;
    }

    @Override
    public LineItem compute(SurchargeContext context, SurchargeRate rateRow) {
        BigDecimal amount = context.baseFreightResult().baseFreightAmount()
                .multiply(rateRow.getRateValue())
                .setScale(2, RoundingMode.HALF_UP);
        return new LineItem("FRAGILE_HANDLING", "Fragile handling", amount, amount);
    }
}
