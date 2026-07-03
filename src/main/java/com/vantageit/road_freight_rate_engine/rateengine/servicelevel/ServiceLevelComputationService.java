package com.vantageit.road_freight_rate_engine.rateengine.servicelevel;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.LineItem;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.RateComputeRequest;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.ServiceLevel;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.ServiceRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pipeline Stage 7: service level multiplier & accessorial charges.
 *
 * <p><b>Precondition: the request must have already passed Stage 3 validation.</b> This service
 * is a pure consumer of {@link PreMultiplierTotals} — it does not know or depend on how
 * {@code surchargesTotal}/{@code clearancesTotal} were computed, since those pipeline stages
 * (surcharges, clearances) don't exist in this codebase yet.
 *
 * <p><b>Currency</b>: {@link PreMultiplierTotals#currency()} is treated as the authoritative
 * currency for this whole computation — carried through to {@link ServiceLevelResult#currency()}
 * unconverted, same "carry through, don't convert" approach Stage 6 takes for
 * {@code road_freight_rates.currency}. Unlike Stage 6 (which only ever resolves one rate row at a
 * time), this stage combines multiple independently-priced inputs (base freight, surcharges,
 * clearances, and now accessorial {@code surcharge_rates} rows) into one running total, so a
 * mismatched accessorial currency is actively rejected via {@link AccessorialCurrencyMismatchException}
 * rather than silently summed — see {@link AccessorialChargeCalculator}. Actually converting
 * between valid currencies remains Stage 8's job, not this stage's.
 *
 * <p><b>Rounding</b>: every amount this service produces or returns is rounded to 2 decimal
 * places ({@link RoundingMode#HALF_UP}) — {@code multiply()} never rounds on its own, so without
 * an explicit step here a "1.15×" multiplier against a 2dp total produces an invalid 4dp currency
 * value (e.g. {@code 383.3295}), not a valid payable ZAR amount.
 */
@Service
@Transactional(readOnly = true)
public class ServiceLevelComputationService {

    private final AccessorialChargeCalculator accessorialChargeCalculator;

    public ServiceLevelComputationService(AccessorialChargeCalculator accessorialChargeCalculator) {
        this.accessorialChargeCalculator = accessorialChargeCalculator;
    }

    public ServiceLevelResult compute(PreMultiplierTotals totals, RateComputeRequest request) {
        ServiceRequest service = request.service();
        BigDecimal multiplier = ServiceLevelMultiplierResolver.resolve(service.serviceLevel());

        // Both operands rounded to 2dp before the subtraction below, so the displayed "before"
        // total, the displayed uplift, and the displayed "after" total always reconcile exactly at
        // the precision the customer actually sees — rather than each being independently rounded
        // and potentially off by a cent from each other.
        BigDecimal preMultiplierSum = totals.sum().setScale(2, RoundingMode.HALF_UP);
        BigDecimal multipliedSubtotal = preMultiplierSum.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
        BigDecimal upliftAmount = multipliedSubtotal.subtract(preMultiplierSum);

        LineItem serviceLevelLineItem = new LineItem(
                "SERVICE_MULTIPLIER",
                "%s service uplift (%s×)".formatted(capitalize(service.serviceLevel()), multiplier),
                BigDecimal.ZERO,
                upliftAmount);

        // request.rateDate() is used as the "as of" date consistently with every other rate-table
        // lookup in this codebase (Stage 6's RateRowResolver, Stage 2's active-rate repositories),
        // rather than service.collectionDate() — a domain date, not a pricing-effective-date concept.
        List<LineItem> accessorialLineItems = accessorialChargeCalculator.compute(service, request.rateDate(), totals.currency());
        BigDecimal accessorialSum = accessorialLineItems.stream()
                .map(LineItem::sellZar)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal runningTotal = multipliedSubtotal.add(accessorialSum);

        return new ServiceLevelResult(multipliedSubtotal, totals.currency(), serviceLevelLineItem, accessorialLineItems, runningTotal);
    }

    private static String capitalize(ServiceLevel serviceLevel) {
        String name = serviceLevel.name();
        return name.charAt(0) + name.substring(1).toLowerCase(Locale.ROOT);
    }
}
