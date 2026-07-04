package com.vantageit.road_freight_rate_engine.rateengine.clearance;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.LineItem;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.RouteType;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.BorderPost;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.SurchargeRate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Road access fee (ZINARA/DNA) — triggers specifically on entry into Zimbabwe or Mozambique, per
 * the doc. Derived from {@link BorderPost#getDestinationCountry()}, resolved once by {@link
 * ClearanceComputationService} and passed via {@link ClearanceContext}.
 */
@Component
public class ZinaraRoadAccessFeeRule implements ClearanceRule {

    /** Same set as {@link CarbonTaxLevyRule} — intentionally identical, both are ZIM/MOZ-gated. */
    private static final Set<String> ZIM_MOZ_COUNTRIES = Set.of("ZW", "MZ");

    @Override
    public String surchargeCode() {
        return "ZINARA_ROAD_ACCESS_FEE";
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
        return new LineItem("ZINARA_ROAD_ACCESS_FEE", "Road access fee (ZINARA/DNA)", amount, amount);
    }
}
