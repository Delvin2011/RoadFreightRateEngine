package com.vantageit.road_freight_rate_engine.rateengine.surcharge;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.LineItem;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.RateComputeRequest;
import com.vantageit.road_freight_rate_engine.rateengine.basefreight.BaseFreightResult;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.SurchargeRate;
import com.vantageit.road_freight_rate_engine.rateengine.domain.repository.SurchargeRateRepository;
import com.vantageit.road_freight_rate_engine.rateengine.laneresolution.LaneResolutionResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pipeline Stage 5: surcharge stack computation.
 *
 * <p>Looks up each applicable rule's active {@code surcharge_rates} row by {@code surcharge_code}
 * and {@code collection_date} (not {@code rate_date}, unlike Stages 6/7 — an explicit choice for
 * this stage per its own spec; flagged here since it's an inconsistency worth knowing about, not
 * an oversight).
 *
 * <p><b>Does not implement the full architecture-spec surcharge catalogue.</b> The following are
 * out of scope, each needing data or request fields that don't exist yet — see the {@code
 * surcharge_catalogue_deferred_items} project memory for the consolidated record:
 * <ul>
 *   <li>Weekend/public holiday surcharge — needs a SA public holiday calendar.</li>
 *   <li>Remote area collection/delivery — needs a "remote" address classification, not in {@link
 *       com.vantageit.road_freight_rate_engine.rateengine.api.dto.AddressType}.</li>
 *   <li>Detention, multiple drop — need post-collection operational facts (stop count, actual
 *       dock time) not in the request model, same category as the already-deferred waiting-time
 *       gap.</li>
 *   <li>Hazmat class-specific surcharges (Class 1/2/3/4/5/6.1/6.2/8/9) — needs UN-number-to-ADG-class
 *       reference data that doesn't exist.</li>
 *   <li>Reefer pre-cooling — needs a "pre-cool required" flag not in the request model.</li>
 *   <li>Oversized/abnormal load permit-per-province — needs province-traversal computation, not
 *       modeled.</li>
 *   <li>Police escort per-day — needs escort duration, not modeled.</li>
 *   <li>Phytosanitary certificate — flagged as <b>ambiguous</b>, not just missing: "fresh produce
 *       crossing international border" isn't cleanly derivable from existing fields.
 *       {@code cargo_class = PERISHABLE} + {@code route_type = CROSS_BORDER} is a plausible but
 *       unconfirmed proxy.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class SurchargeStackComputationService {

    private final List<SurchargeRule> rules;
    private final SurchargeRateRepository surchargeRateRepository;

    public SurchargeStackComputationService(List<SurchargeRule> rules, SurchargeRateRepository surchargeRateRepository) {
        this.rules = rules;
        this.surchargeRateRepository = surchargeRateRepository;
    }

    public SurchargeStackResult compute(RateComputeRequest request, BaseFreightResult baseFreightResult, LaneResolutionResult lane) {
        SurchargeContext context = new SurchargeContext(request, baseFreightResult, lane);
        LocalDate asOfDate = request.service().collectionDate();

        List<LineItem> lineItems = new ArrayList<>();
        for (SurchargeRule rule : rules) {
            if (!rule.isApplicable(context)) {
                continue;
            }
            SurchargeRate rateRow = surchargeRateRepository.findActiveSurcharges(rule.surchargeCode(), asOfDate)
                    .orElseThrow(() -> new SurchargeRateNotFoundException(rule.surchargeCode(), asOfDate));
            lineItems.add(rule.compute(context, rateRow));
        }

        BigDecimal surchargesTotal = lineItems.stream()
                .map(LineItem::sellZar)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new SurchargeStackResult(lineItems, surchargesTotal);
    }
}
