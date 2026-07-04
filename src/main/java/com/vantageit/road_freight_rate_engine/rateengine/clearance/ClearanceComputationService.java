package com.vantageit.road_freight_rate_engine.rateengine.clearance;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.LineItem;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.RateComputeRequest;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.RouteType;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.BorderPost;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.SurchargeRate;
import com.vantageit.road_freight_rate_engine.rateengine.domain.repository.BorderPostRepository;
import com.vantageit.road_freight_rate_engine.rateengine.domain.repository.SurchargeRateRepository;
import com.vantageit.road_freight_rate_engine.rateengine.laneresolution.LaneResolutionResult;
import com.vantageit.road_freight_rate_engine.rateengine.surcharge.SurchargeRateNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pipeline Stage 6: clearance & compliance charges.
 *
 * <p>Looks up each applicable rule's active {@code surcharge_rates} row by {@code surcharge_code}
 * and {@code collection_date} — confirmed against Stage 9's review as the canonical field for
 * this kind of catalogue-driven lookup, not decided independently here.
 *
 * <p><b>Does not implement the full architecture-spec clearance/compliance catalogue.</b> Three
 * gaps were already recorded during Stage 9 and are not duplicated here — see the {@code
 * surcharge_catalogue_deferred_items} project memory, which now also cross-references this stage:
 * <ul>
 *   <li>Abnormal load permit — needs province-traversal computation, not modeled.</li>
 *   <li>Police escort booking fee — needs escort duration, not modeled.</li>
 *   <li>DAFF phytosanitary certificate — ambiguous "fresh produce crossing international border"
 *       trigger, not cleanly derivable.</li>
 * </ul>
 * One new gap found in this stage, added to the same memory: <b>foreign transit permit</b> — the
 * doc's trigger is "transit through a third country" (e.g. transiting Zimbabwe en route to
 * Zambia), and there's no field modeling multi-country transit vs. a direct border crossing.
 *
 * <p><b>{@code CLEARING_FEE_REQUIRED}</b>: a cross-border quote without a {@code
 * BORDER_CLEARING_AGENT_FEE} line item is a business-rule violation, not just a missing optional
 * charge (see {@link ClearingFeeRequiredException}). Enforced as a defensive final check after the
 * rule loop, not by letting a missing rate row for this one code throw the generic {@link
 * SurchargeRateNotFoundException} every other rule uses — {@link BorderClearingAgentFeeRule}'s
 * missing-rate case is special-cased to fall through to that check instead.
 */
@Service
@Transactional(readOnly = true)
public class ClearanceComputationService {

    private final List<ClearanceRule> rules;
    private final SurchargeRateRepository surchargeRateRepository;
    private final BorderPostRepository borderPostRepository;

    public ClearanceComputationService(
            List<ClearanceRule> rules, SurchargeRateRepository surchargeRateRepository, BorderPostRepository borderPostRepository) {
        this.rules = rules;
        this.surchargeRateRepository = surchargeRateRepository;
        this.borderPostRepository = borderPostRepository;
    }

    public ClearanceResult compute(RateComputeRequest request, LaneResolutionResult lane) {
        BorderPost borderPost = resolveBorderPost(request);
        ClearanceContext context = new ClearanceContext(request, lane, borderPost);
        LocalDate asOfDate = request.service().collectionDate();

        List<LineItem> lineItems = new ArrayList<>();
        for (ClearanceRule rule : rules) {
            if (!rule.isApplicable(context)) {
                continue;
            }
            Optional<SurchargeRate> rateRow = surchargeRateRepository.findActiveSurcharges(rule.surchargeCode(), asOfDate);
            if (rateRow.isEmpty()) {
                if (BorderClearingAgentFeeRule.SURCHARGE_CODE.equals(rule.surchargeCode())) {
                    // Handled by the CLEARING_FEE_REQUIRED check below instead of the generic
                    // SurchargeRateNotFoundException every other rule uses.
                    continue;
                }
                throw new SurchargeRateNotFoundException(rule.surchargeCode(), asOfDate);
            }
            lineItems.add(rule.compute(context, rateRow.get()));
        }

        if (request.route().routeType() == RouteType.CROSS_BORDER
                && lineItems.stream().noneMatch(li -> BorderClearingAgentFeeRule.SURCHARGE_CODE.equals(li.code()))) {
            throw new ClearingFeeRequiredException(asOfDate);
        }

        BigDecimal clearancesTotal = lineItems.stream()
                .map(LineItem::sellZar)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ClearanceResult(lineItems, clearancesTotal);
    }

    private BorderPost resolveBorderPost(RateComputeRequest request) {
        if (request.route().routeType() != RouteType.CROSS_BORDER || request.route().borderPostId() == null) {
            return null;
        }
        return borderPostRepository.findById(request.route().borderPostId()).orElse(null);
    }
}
