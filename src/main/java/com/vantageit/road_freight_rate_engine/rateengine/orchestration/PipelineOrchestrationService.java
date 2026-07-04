package com.vantageit.road_freight_rate_engine.rateengine.orchestration;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.LineItem;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.RateComputeRequest;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.RateComputeResponse;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.ValidationError;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.ValidationErrorResponse;
import com.vantageit.road_freight_rate_engine.rateengine.basefreight.BaseFreightComputationService;
import com.vantageit.road_freight_rate_engine.rateengine.basefreight.BaseFreightResult;
import com.vantageit.road_freight_rate_engine.rateengine.clearance.ClearanceComputationService;
import com.vantageit.road_freight_rate_engine.rateengine.clearance.ClearanceResult;
import com.vantageit.road_freight_rate_engine.rateengine.currencyconversion.CurrencyConversionResult;
import com.vantageit.road_freight_rate_engine.rateengine.currencyconversion.CurrencyConversionService;
import com.vantageit.road_freight_rate_engine.rateengine.currencyconversion.MonetaryLineItem;
import com.vantageit.road_freight_rate_engine.rateengine.currencyconversion.TotalsComputationService;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.SurchargeRate;
import com.vantageit.road_freight_rate_engine.rateengine.domain.repository.SurchargeRateRepository;
import com.vantageit.road_freight_rate_engine.rateengine.laneresolution.LaneResolutionResult;
import com.vantageit.road_freight_rate_engine.rateengine.laneresolution.LaneResolutionService;
import com.vantageit.road_freight_rate_engine.rateengine.pipeline.common.ChargeableWeightCalculator;
import com.vantageit.road_freight_rate_engine.rateengine.servicelevel.PreMultiplierTotals;
import com.vantageit.road_freight_rate_engine.rateengine.servicelevel.ServiceLevelComputationService;
import com.vantageit.road_freight_rate_engine.rateengine.servicelevel.ServiceLevelResult;
import com.vantageit.road_freight_rate_engine.rateengine.surcharge.SurchargeStackComputationService;
import com.vantageit.road_freight_rate_engine.rateengine.surcharge.SurchargeStackResult;
import com.vantageit.road_freight_rate_engine.rateengine.validation.InputValidationService;
import com.vantageit.road_freight_rate_engine.rateengine.validation.ValidationResult;
import com.vantageit.road_freight_rate_engine.rateengine.vehicleselection.VehicleSelectionResult;
import com.vantageit.road_freight_rate_engine.rateengine.vehicleselection.VehicleSelectionService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Wires every pipeline stage together in sequence, all-or-nothing: any failure (Stage 1 validation
 * or any downstream stage exception) means no {@link RateComputeResponse} is returned at all.
 *
 * <p>Execution order is deliberate, not the doc's literal stage-number order — see each step's
 * comment in {@link #computePipeline}. This class wires and translates only; it must never contain
 * pricing/business logic of its own — every computation belongs to, and stays in, the stage
 * service that owns it.
 *
 * <p><b>Currency reconciliation happens exactly once</b>, after every stage that produces a line
 * item has run (base freight, surcharges, clearances, service level + accessorials), not per-stage
 * and not earlier.
 *
 * <p><b>Known, tracked limitation, worked around pragmatically here, not fixed</b> (see the {@code
 * catalogue_currency_consistency_followup} project memory): Stage 9's {@code SurchargeStackResult}
 * and Stage 10's {@code ClearanceResult} return plain {@link LineItem}s with no per-item currency —
 * each stage's own {@code surchargesTotal}/{@code clearancesTotal} is a raw, currency-blind sum.
 * Rather than modifying either stage's internals (explicitly out of scope for this task), this
 * class independently re-queries each surcharge/clearance line item's own {@code surcharge_rates}
 * row by its code and {@code collection_date} (the same lookup key the originating stage already
 * used) to recover its real currency for the final conversion pass in step 8 — so the *final*
 * customer-facing totals are still correctly reconciled even though the intermediate per-stage
 * totals remain (as tracked) currency-blind.
 *
 * <p><b>Subtlety within that workaround</b>: a handful of surcharge codes ({@code FUEL_LEVY},
 * {@code HAZMAT_PG1_UPLIFT}, {@code FRAGILE_HANDLING}, {@code NON_STACKABLE_SPACE_FACTOR}) are
 * percentages <i>of BASE_FREIGHT</i>, not flat/per-km amounts in their own right — their line item's
 * real currency is whatever BASE_FREIGHT's currency is, not whatever currency happens to be on
 * their own {@code surcharge_rates} row (that row's currency is really just metadata for the
 * percentage value itself, not the resulting amount's unit). {@code FROZEN_GOODS_UPLIFT} has the
 * same issue one level removed — it's a percentage of {@code REEFER_RUNNING}'s amount, so it
 * inherits currency from {@code REEFER_RUNNING}'s own row, not its own. Every other surcharge/clearance
 * code (flat or per-km amounts, plus {@code HIGH_VALUE_INSURANCE_LEVY}, a percentage of
 * {@code declared_value_zar} — a field that's ZAR by DTO convention regardless of base freight)
 * correctly uses its own row's currency. Getting this wrong is not hypothetical: found by
 * constructing a genuine EUR-base-freight test case, where treating {@code FUEL_LEVY} as ZAR
 * (its own row's currency) instead of EUR (what it's actually 22% of) understated the true total
 * by roughly a factor of 20.
 *
 * <p><b>{@code rate_snapshot_id}</b> is a random UUID generated per computation — a placeholder.
 * It does <b>not</b> yet capture the actual set of rate table row versions used for this
 * computation; real audit snapshotting is separate, deferred scope.
 *
 * <p><b>{@code flags}/{@code requires_manual_review}</b> are populated only from what already
 * exists: Stage 1's {@code ValidationResult.flags()} and Stage 2's {@code distanceOverrideApplied()}
 * (→ {@code DISTANCE_OVERRIDE}). The Class 7/Class 1 hard blocks and the
 * {@code PROJECT_CARGO}/{@code HIGH_VALUE_OVER_2M}/{@code ABNORMAL_LOAD}/{@code LIVE_ANIMALS}
 * manual-review flags were never built by any stage and are separate, future scope — {@code
 * requires_manual_review} is unconditionally {@code false} pending that work.
 */
@Service
@Transactional(readOnly = true)
public class PipelineOrchestrationService {

    private static final String SUCCESS_STATUS = "success";
    private static final String ERROR_STATUS = "error";

    private final InputValidationService inputValidationService;
    private final LaneResolutionService laneResolutionService;
    private final VehicleSelectionService vehicleSelectionService;
    private final BaseFreightComputationService baseFreightComputationService;
    private final SurchargeStackComputationService surchargeStackComputationService;
    private final ClearanceComputationService clearanceComputationService;
    private final ServiceLevelComputationService serviceLevelComputationService;
    private final CurrencyConversionService currencyConversionService;
    private final TotalsComputationService totalsComputationService;
    private final SurchargeRateRepository surchargeRateRepository;
    private final PipelineExceptionTranslator exceptionTranslator;

    public PipelineOrchestrationService(
            InputValidationService inputValidationService,
            LaneResolutionService laneResolutionService,
            VehicleSelectionService vehicleSelectionService,
            BaseFreightComputationService baseFreightComputationService,
            SurchargeStackComputationService surchargeStackComputationService,
            ClearanceComputationService clearanceComputationService,
            ServiceLevelComputationService serviceLevelComputationService,
            CurrencyConversionService currencyConversionService,
            TotalsComputationService totalsComputationService,
            SurchargeRateRepository surchargeRateRepository,
            PipelineExceptionTranslator exceptionTranslator) {
        this.inputValidationService = inputValidationService;
        this.laneResolutionService = laneResolutionService;
        this.vehicleSelectionService = vehicleSelectionService;
        this.baseFreightComputationService = baseFreightComputationService;
        this.surchargeStackComputationService = surchargeStackComputationService;
        this.clearanceComputationService = clearanceComputationService;
        this.serviceLevelComputationService = serviceLevelComputationService;
        this.currencyConversionService = currencyConversionService;
        this.totalsComputationService = totalsComputationService;
        this.surchargeRateRepository = surchargeRateRepository;
        this.exceptionTranslator = exceptionTranslator;
    }

    public RateComputeResponse compute(RateComputeRequest request) {
        // Step 1: if invalid, stop immediately -- no later stage is ever invoked.
        ValidationResult validationResult = inputValidationService.validate(request);
        if (!validationResult.isValid()) {
            throw new PipelineValidationException(new ValidationErrorResponse(ERROR_STATUS, validationResult.errors()));
        }

        try {
            return computePipeline(request, validationResult);
        } catch (RuntimeException ex) {
            ValidationError error = exceptionTranslator.translate(ex);
            throw new PipelineValidationException(new ValidationErrorResponse(ERROR_STATUS, List.of(error)));
        }
    }

    private RateComputeResponse computePipeline(RateComputeRequest request, ValidationResult validationResult) {
        // Steps 2-4: lane, vehicle, base freight.
        LaneResolutionResult lane = laneResolutionService.resolve(request);
        VehicleSelectionResult vehicle = vehicleSelectionService.selectVehicle(request, lane);
        BaseFreightResult baseFreight = baseFreightComputationService.compute(request, lane, vehicle);

        // Steps 5-6: surcharges, clearances.
        SurchargeStackResult surcharges = surchargeStackComputationService.compute(request, baseFreight, lane);
        ClearanceResult clearances = clearanceComputationService.compute(request, lane);

        // Step 7: assemble PreMultiplierTotals, run the service-level multiplier + accessorials.
        // baseFreight.currency() is the single-currency assumption PreMultiplierTotals itself
        // documents -- the pragmatic workaround for surcharges/clearances described in this
        // class's Javadoc lives in buildMonetaryLineItems below, not here.
        PreMultiplierTotals preMultiplierTotals = new PreMultiplierTotals(
                baseFreight.baseFreightAmount(), surcharges.surchargesTotal(), clearances.clearancesTotal(), baseFreight.currency());
        ServiceLevelResult serviceLevel = serviceLevelComputationService.compute(preMultiplierTotals, request);

        LocalDate collectionDate = request.service().collectionDate();

        // Step 8: currency reconciliation, done once, here -- every LineItem from steps 2-7 in one pass.
        List<MonetaryLineItem> monetaryLineItems = buildMonetaryLineItems(baseFreight, surcharges, clearances, serviceLevel, collectionDate);
        CurrencyConversionResult conversionResult = currencyConversionService.convert(monetaryLineItems, collectionDate);

        // Step 9: TotalsComputationService on the converted list -- reused for its subtotal/VAT
        // arithmetic rather than reimplementing it here. Everything is already ZAR by this point,
        // so its own internal conversion call is a harmless no-op pass-through.
        List<MonetaryLineItem> zarLineItems = conversionResult.convertedLineItems().stream()
                .map(li -> new MonetaryLineItem(li.code(), li.description(), li.buyZar(), li.sellZar(), "ZAR"))
                .toList();
        TotalsComputationService.TotalsResult totalsResult = totalsComputationService.compute(zarLineItems, collectionDate);

        // Step 10: assemble the final response.
        BigDecimal chargeableWeightKg = ChargeableWeightCalculator.compute(request.cargo().grossWeightKg(), request.cargo().volumeCbm());

        List<String> flags = new ArrayList<>(validationResult.flags());
        if (lane.distanceOverrideApplied()) {
            flags.add("DISTANCE_OVERRIDE");
        }

        return new RateComputeResponse(
                SUCCESS_STATUS,
                request.quoteContextId(),
                UUID.randomUUID(), // rate_snapshot_id placeholder -- see class Javadoc
                Instant.now(),
                vehicle.selectedVehicleCategoryCode(),
                lane.distanceKm(),
                chargeableWeightKg,
                false, // requires_manual_review -- incomplete pending future business rules, see class Javadoc
                flags,
                totalsResult.convertedLineItems(),
                totalsResult.totals(),
                conversionResult.exchangeRatesUsed());
    }

    // Percentage-of-BASE_FREIGHT surcharges -- see class Javadoc's "Subtlety" section. Their
    // effective currency is BASE_FREIGHT's, not their own surcharge_rates row's.
    private static final Set<String> PCT_OF_BASE_FREIGHT_CODES =
            Set.of("FUEL_LEVY", "HAZMAT_PG1_UPLIFT", "FRAGILE_HANDLING", "NON_STACKABLE_SPACE_FACTOR");
    private static final String FROZEN_GOODS_UPLIFT_CODE = "FROZEN_GOODS_UPLIFT";
    private static final String REEFER_RUNNING_CODE = "REEFER_RUNNING";

    private List<MonetaryLineItem> buildMonetaryLineItems(
            BaseFreightResult baseFreight, SurchargeStackResult surcharges, ClearanceResult clearances,
            ServiceLevelResult serviceLevel, LocalDate collectionDate) {
        List<MonetaryLineItem> items = new ArrayList<>();

        String baseFreightDescription = baseFreight.lineItemComment() == null
                ? "Base freight"
                : "Base freight (%s)".formatted(baseFreight.lineItemComment());
        items.add(new MonetaryLineItem(
                "BASE_FREIGHT", baseFreightDescription, baseFreight.baseFreightAmount(), baseFreight.baseFreightAmount(), baseFreight.currency()));

        for (LineItem lineItem : surcharges.lineItems()) {
            items.add(toMonetaryLineItem(lineItem, resolveSurchargeCurrency(lineItem.code(), collectionDate, baseFreight.currency())));
        }
        for (LineItem lineItem : clearances.lineItems()) {
            items.add(toMonetaryLineItem(lineItem, resolveSurchargeCurrency(lineItem.code(), collectionDate, baseFreight.currency())));
        }

        // Stage 7 already validates every accessorial's currency matches serviceLevel.currency()
        // (AccessorialCurrencyMismatchException otherwise) -- no independent re-lookup needed here.
        items.add(toMonetaryLineItem(serviceLevel.serviceLevelLineItem(), serviceLevel.currency()));
        for (LineItem lineItem : serviceLevel.accessorialLineItems()) {
            items.add(toMonetaryLineItem(lineItem, serviceLevel.currency()));
        }

        return items;
    }

    private static MonetaryLineItem toMonetaryLineItem(LineItem lineItem, String currency) {
        return new MonetaryLineItem(lineItem.code(), lineItem.description(), lineItem.buyZar(), lineItem.sellZar(), currency);
    }

    /**
     * See this class's Javadoc for why this re-query exists and for the percentage-of-something-else
     * subtlety it has to account for.
     */
    private String resolveSurchargeCurrency(String surchargeCode, LocalDate collectionDate, String baseFreightCurrency) {
        if (PCT_OF_BASE_FREIGHT_CODES.contains(surchargeCode)) {
            return baseFreightCurrency;
        }
        if (FROZEN_GOODS_UPLIFT_CODE.equals(surchargeCode)) {
            return resolveSurchargeRateCurrency(REEFER_RUNNING_CODE, collectionDate);
        }
        return resolveSurchargeRateCurrency(surchargeCode, collectionDate);
    }

    /**
     * Guaranteed at most one active row for a given code + date: the originating stage (Stage 9 or
     * 10) already called this exact repository method with this exact code and date and succeeded
     * (that's how this line item came to exist at all) — if more than one active row matched,
     * {@code findActiveSurcharges}'s {@code Optional<T>} return type means Spring Data would have
     * already thrown {@code IncorrectResultSizeDataAccessException} on that earlier call, before
     * this line item was ever produced. Both calls happen inside the same read-only transaction
     * (see this class's {@code @Transactional}), so the result can't have changed in between.
     */
    private String resolveSurchargeRateCurrency(String surchargeCode, LocalDate collectionDate) {
        return surchargeRateRepository.findActiveSurcharges(surchargeCode, collectionDate)
                .map(SurchargeRate::getCurrency)
                .orElseThrow(() -> new IllegalStateException(
                        "Surcharge rate for %s vanished between stage computation and currency resolution".formatted(surchargeCode)));
    }
}
