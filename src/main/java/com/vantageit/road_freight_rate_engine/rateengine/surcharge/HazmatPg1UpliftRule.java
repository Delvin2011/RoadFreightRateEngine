package com.vantageit.road_freight_rate_engine.rateengine.surcharge;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.LineItem;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.PackingGroup;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.SurchargeRate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/**
 * Applies when {@code cargo.hazmat_packing_group == I}. Separate from the ADG-class-specific
 * hazmat surcharges (out of scope — see {@code surcharge_catalogue_deferred_items} memory), which
 * need UN-number-to-ADG-class reference data that doesn't exist. This one only needs the packing
 * group field, which does exist on the request.
 */
@Component
public class HazmatPg1UpliftRule implements SurchargeRule {

    @Override
    public String surchargeCode() {
        return "HAZMAT_PG1_UPLIFT";
    }

    @Override
    public boolean isApplicable(SurchargeContext context) {
        return context.request().cargo().hazmatPackingGroup() == PackingGroup.I;
    }

    @Override
    public LineItem compute(SurchargeContext context, SurchargeRate rateRow) {
        BigDecimal amount = context.baseFreightResult().baseFreightAmount()
                .multiply(rateRow.getRateValue())
                .setScale(2, RoundingMode.HALF_UP);
        return new LineItem("HAZMAT_PG1_UPLIFT", "Hazmat packing group I uplift", amount, amount);
    }
}
