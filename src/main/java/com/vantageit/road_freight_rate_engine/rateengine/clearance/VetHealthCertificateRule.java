package com.vantageit.road_freight_rate_engine.rateengine.clearance;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.LineItem;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.SurchargeRate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/** Applies when {@code cargo.live_animals == true}, independent of route type. Flat fee. */
@Component
public class VetHealthCertificateRule implements ClearanceRule {

    @Override
    public String surchargeCode() {
        return "VET_HEALTH_CERTIFICATE";
    }

    @Override
    public boolean isApplicable(ClearanceContext context) {
        return Boolean.TRUE.equals(context.request().cargo().liveAnimals());
    }

    @Override
    public LineItem compute(ClearanceContext context, SurchargeRate rateRow) {
        BigDecimal amount = rateRow.getRateValue().setScale(2, RoundingMode.HALF_UP);
        return new LineItem("VET_HEALTH_CERTIFICATE", "Veterinary health certificate", amount, amount);
    }
}
