package com.vantageit.road_freight_rate_engine.rateengine.clearance;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.CargoClass;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.LineItem;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.SurchargeRate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/** Applies when {@code cargo.cargo_class == HAZMAT}, independent of route type. Flat fee. */
@Component
public class AdgTransportPermitRule implements ClearanceRule {

    @Override
    public String surchargeCode() {
        return "ADG_TRANSPORT_PERMIT";
    }

    @Override
    public boolean isApplicable(ClearanceContext context) {
        return context.request().cargo().cargoClass() == CargoClass.HAZMAT;
    }

    @Override
    public LineItem compute(ClearanceContext context, SurchargeRate rateRow) {
        BigDecimal amount = rateRow.getRateValue().setScale(2, RoundingMode.HALF_UP);
        return new LineItem("ADG_TRANSPORT_PERMIT", "ADG transport permit", amount, amount);
    }
}
