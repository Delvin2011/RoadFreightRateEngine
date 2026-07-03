package com.vantageit.road_freight_rate_engine.rateengine.servicelevel;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.LineItem;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.ServiceRequest;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.SurchargeRate;
import com.vantageit.road_freight_rate_engine.rateengine.domain.repository.SurchargeRateRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Computes accessorial line items from {@link ServiceRequest}'s boolean flags, sourcing flat
 * amounts from {@code surcharge_rates} ({@code SurchargeRateRepository}, built in Stage 2) — never
 * hardcoded, even though the surcharges pipeline stage itself isn't built yet.
 *
 * <p><b>No date logic of its own</b>: {@code asOfDate} is passed straight through to {@link
 * SurchargeRateRepository#findActiveSurcharges(String, LocalDate)} unchanged — every bit of
 * effective_from/effective_to bounding happens in that repository query, not here. A calculator-
 * level date-boundary test would only re-prove the same repository behavior through an extra layer
 * of indirection; the repository-level test is the correct place for that coverage.
 *
 * <p>Every returned {@link LineItem} amount is rounded to 2 decimal places
 * ({@link RoundingMode#HALF_UP}) — {@code surcharge_rates.rate_value} is stored at 4dp precision,
 * but ZAR (and every other currency this codebase supports) only has 2 decimal places as legal
 * tender, so the raw stored value is not itself a valid payable amount.
 *
 * <p><b>Known model limitations, flagged rather than guessed:</b>
 * <ul>
 *   <li>"Waiting time" is listed as an accessorial in the spec, but isn't implemented here. Not
 *       simply a missing {@link ServiceRequest} flag: its trigger condition ("declared at booking
 *       — exceeds 1hr free allowance") suggests the actual chargeable amount depends on real
 *       waiting duration, only known after the collection/delivery event — likely a post-event
 *       billing concern outside a pre-shipment quote engine's scope, not an oversight here. See
 *       the {@code accessorial_waiting_time_scope_question} project memory; needs a business
 *       decision, not more engineering, to close out.</li>
 * </ul>
 */
@Component
public class AccessorialChargeCalculator {

    private final SurchargeRateRepository surchargeRateRepository;

    public AccessorialChargeCalculator(SurchargeRateRepository surchargeRateRepository) {
        this.surchargeRateRepository = surchargeRateRepository;
    }

    public List<LineItem> compute(ServiceRequest service, LocalDate asOfDate, String expectedCurrency) {
        List<LineItem> lineItems = new ArrayList<>();

        if (Boolean.TRUE.equals(service.afterHoursCollection())) {
            lineItems.add(lineItemFor("AFTER_HOURS_COLLECTION", "After-hours collection", asOfDate, expectedCurrency));
        }
        if (Boolean.TRUE.equals(service.afterHoursDelivery())) {
            lineItems.add(lineItemFor("AFTER_HOURS_DELIVERY", "After-hours delivery", asOfDate, expectedCurrency));
        }
        if (Boolean.TRUE.equals(service.tailLiftCollection())) {
            lineItems.add(lineItemFor("TAIL_LIFT_COLLECTION", "Tail lift — collection", asOfDate, expectedCurrency));
        }
        if (Boolean.TRUE.equals(service.tailLiftDelivery())) {
            lineItems.add(lineItemFor("TAIL_LIFT_DELIVERY", "Tail lift — delivery", asOfDate, expectedCurrency));
        }
        if (Boolean.TRUE.equals(service.driverAssistLoading())) {
            lineItems.add(lineItemFor("DRIVER_ASSIST_LOADING", "Driver assist — loading", asOfDate, expectedCurrency));
        }
        if (Boolean.TRUE.equals(service.driverAssistOffloading())) {
            lineItems.add(lineItemFor("DRIVER_ASSIST_OFFLOADING", "Driver assist — offloading", asOfDate, expectedCurrency));
        }

        return lineItems;
    }

    private LineItem lineItemFor(String surchargeCode, String description, LocalDate asOfDate, String expectedCurrency) {
        SurchargeRate rate = surchargeRateRepository.findActiveSurcharges(surchargeCode, asOfDate)
                .orElseThrow(() -> new AccessorialRateNotFoundException(surchargeCode, asOfDate));
        if (!rate.getCurrency().equals(expectedCurrency)) {
            throw new AccessorialCurrencyMismatchException(surchargeCode, expectedCurrency, rate.getCurrency());
        }
        // surcharge_rates has a single rate_value, no separate buy/sell — same as every other
        // flat-rate line item this codebase produces.
        BigDecimal amount = rate.getRateValue().setScale(2, RoundingMode.HALF_UP);
        return new LineItem(surchargeCode, description, amount, amount);
    }
}
