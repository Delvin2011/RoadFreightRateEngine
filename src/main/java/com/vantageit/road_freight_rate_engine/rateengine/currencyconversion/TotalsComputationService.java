package com.vantageit.road_freight_rate_engine.rateengine.currencyconversion;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.LineItem;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.Totals;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates Steps 2 and 3 of pipeline Stage 8: converts every line item to ZAR, sums to a
 * subtotal, applies VAT, and produces the final {@link Totals}.
 *
 * <p>Takes a plain {@code asOfDate}, not a {@code RateComputeRequest} — same decoupling discipline
 * as {@link CurrencyConversionService}. The caller is expected to pass {@code
 * request.service().collectionDate()} (per the architecture spec's own guidance for this stage),
 * but this service has no dependency on the request DTO shape itself.
 */
@Service
@Transactional(readOnly = true)
public class TotalsComputationService {

    private final CurrencyConversionService currencyConversionService;
    private final VatCalculationService vatCalculationService;

    public TotalsComputationService(CurrencyConversionService currencyConversionService, VatCalculationService vatCalculationService) {
        this.currencyConversionService = currencyConversionService;
        this.vatCalculationService = vatCalculationService;
    }

    public record TotalsResult(Totals totals, List<LineItem> convertedLineItems, Map<String, BigDecimal> exchangeRatesUsed) {
    }

    public TotalsResult compute(List<MonetaryLineItem> lineItems, LocalDate asOfDate) {
        CurrencyConversionResult conversionResult = currencyConversionService.convert(lineItems, asOfDate);

        BigDecimal subtotalBuyZar = conversionResult.convertedLineItems().stream()
                .map(LineItem::buyZar)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal subtotalSellZar = conversionResult.convertedLineItems().stream()
                .map(LineItem::sellZar)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Hardcoded false pending a real input source — see VatCalculationService's Javadoc and the
        // vat_zero_rating_deferred project memory.
        boolean zeroRated = false;
        VatCalculationService.VatResult vatResult = vatCalculationService.computeVat(subtotalSellZar, zeroRated);

        // marginPct is always null here — set by the quotation service, never this engine, per the
        // doc.
        Totals totals = new Totals(subtotalBuyZar, subtotalSellZar, vatResult.vatAmount(), vatResult.totalInclVat(), null);

        return new TotalsResult(totals, conversionResult.convertedLineItems(), conversionResult.exchangeRatesUsed());
    }
}
