package com.vantageit.road_freight_rate_engine.rateengine.clearance;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.LineItem;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.RouteType;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.SurchargeRate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Carbon tax levy (MOZ/ZAM) — same country-derivation approach as {@link ZinaraRoadAccessFeeRule}.
 */
@Component
public class CarbonTaxLevyRule implements ClearanceRule {

    /** Same set as {@link ZinaraRoadAccessFeeRule} — intentionally identical. */
    private static final Set<String> ZIM_MOZ_COUNTRIES = Set.of("ZW", "MZ");

    @Override
    public String surchargeCode() {
        return "CARBON_TAX_LEVY";
    }

    @Override
    public boolean isApplicable(ClearanceContext context) {
        return context.request().route().routeType() == RouteType.CROSS_BORDER
                && context.borderPost() != null
                && ZIM_MOZ_COUNTRIES.contains(context.borderPost().getDestinationCountry());
    }

    @Override
    public LineItem compute(ClearanceContext context, SurchargeRate rateRow) {
        BigDecimal amount = rateRow.getRateValue().setScale(2, RoundingMode.HALF_UP);
        return new LineItem("CARBON_TAX_LEVY", "Carbon tax levy", amount, amount);
    }
}
