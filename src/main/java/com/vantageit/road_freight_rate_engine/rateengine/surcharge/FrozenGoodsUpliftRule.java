package com.vantageit.road_freight_rate_engine.rateengine.surcharge;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.LineItem;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.TemperatureRange;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.SurchargeRate;
import com.vantageit.road_freight_rate_engine.rateengine.domain.repository.SurchargeRateRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * The one surcharge in scope whose base is another surcharge's amount, not BASE_FREIGHT — a
 * documented exception to the "all surcharges apply to BASE_FREIGHT" rule, not a bug. Applies when
 * {@code cargo.temperature_range_c.min < 0°C}.
 *
 * <p>Rather than depending on {@code REEFER_RUNNING} having already been computed elsewhere (which
 * would require either a stateful/mutable {@link SurchargeContext} or a guaranteed rule-ordering
 * assumption in the orchestrator), this rule independently re-resolves {@code REEFER_RUNNING}'s own
 * {@code surcharge_rates} row and recomputes its amount using the identical formula. Whenever this
 * rule is applicable, {@link ReeferRunningRule} is necessarily applicable too (a sub-zero minimum
 * implies {@code temperature_range_c} is set, satisfying {@code ReeferRunningRule}'s OR condition),
 * so the two amounts are always numerically identical to what actually appears as the
 * {@code REEFER_RUNNING} line item — this doesn't risk drifting from it.
 */
@Component
public class FrozenGoodsUpliftRule implements SurchargeRule {

    private final SurchargeRateRepository surchargeRateRepository;

    public FrozenGoodsUpliftRule(SurchargeRateRepository surchargeRateRepository) {
        this.surchargeRateRepository = surchargeRateRepository;
    }

    @Override
    public String surchargeCode() {
        return "FROZEN_GOODS_UPLIFT";
    }

    @Override
    public boolean isApplicable(SurchargeContext context) {
        TemperatureRange range = context.request().cargo().temperatureRangeC();
        return range != null && range.min() != null && range.min().compareTo(BigDecimal.ZERO) < 0;
    }

    @Override
    public LineItem compute(SurchargeContext context, SurchargeRate rateRow) {
        LocalDate asOfDate = context.request().service().collectionDate();
        SurchargeRate reeferRateRow = surchargeRateRepository.findActiveSurcharges("REEFER_RUNNING", asOfDate)
                .orElseThrow(() -> new SurchargeRateNotFoundException("REEFER_RUNNING", asOfDate));

        // Rounded to 2dp here first, matching the concrete dollar figure that actually appears as
        // the REEFER_RUNNING line item — not some unrounded intermediate a reader couldn't verify
        // against the quote.
        BigDecimal reeferRunningAmount = context.lane().distanceKm()
                .multiply(reeferRateRow.getRateValue())
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal amount = reeferRunningAmount
                .multiply(rateRow.getRateValue())
                .setScale(2, RoundingMode.HALF_UP);
        return new LineItem("FROZEN_GOODS_UPLIFT", "Frozen goods uplift", amount, amount);
    }
}
