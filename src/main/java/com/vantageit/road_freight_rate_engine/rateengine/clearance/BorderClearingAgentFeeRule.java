package com.vantageit.road_freight_rate_engine.rateengine.clearance;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.LineItem;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.RouteType;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.SurchargeRate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/**
 * Mandatory whenever {@code route.route_type == CROSS_BORDER} — see the {@code CLEARING_FEE_REQUIRED}
 * business rule enforced by {@link ClearanceComputationService}. Flat fee per crossing.
 */
@Component
public class BorderClearingAgentFeeRule implements ClearanceRule {

    public static final String SURCHARGE_CODE = "BORDER_CLEARING_AGENT_FEE";

    @Override
    public String surchargeCode() {
        return SURCHARGE_CODE;
    }

    @Override
    public boolean isApplicable(ClearanceContext context) {
        return context.request().route().routeType() == RouteType.CROSS_BORDER;
    }

    @Override
    public LineItem compute(ClearanceContext context, SurchargeRate rateRow) {
        BigDecimal amount = rateRow.getRateValue().setScale(2, RoundingMode.HALF_UP);
        return new LineItem(SURCHARGE_CODE, "Border clearing agent fee", amount, amount);
    }
}
