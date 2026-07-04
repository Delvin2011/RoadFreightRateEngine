package com.vantageit.road_freight_rate_engine.rateengine.clearance;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.LineItem;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.RouteType;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.SurchargeRate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/** SARS Customs Processing Fee — always applies for cross-border. */
@Component
public class SarsCpfRule implements ClearanceRule {

    @Override
    public String surchargeCode() {
        return "SARS_CPF";
    }

    @Override
    public boolean isApplicable(ClearanceContext context) {
        return context.request().route().routeType() == RouteType.CROSS_BORDER;
    }

    @Override
    public LineItem compute(ClearanceContext context, SurchargeRate rateRow) {
        BigDecimal amount = rateRow.getRateValue().setScale(2, RoundingMode.HALF_UP);
        return new LineItem("SARS_CPF", "SARS Customs Processing Fee", amount, amount);
    }
}
