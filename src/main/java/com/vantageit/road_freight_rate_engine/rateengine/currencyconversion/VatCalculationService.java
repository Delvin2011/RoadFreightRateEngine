package com.vantageit.road_freight_rate_engine.rateengine.currencyconversion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

/**
 * VAT is a fixed SA statutory rate (15%), not database-configurable — same reasoning as Stage 7's
 * fixed service-level multipliers — so a single named constant is sufficient; only exchange rates
 * come from the DB in this stage.
 */
@Service
public class VatCalculationService {

    public static final BigDecimal VAT_RATE = new BigDecimal("0.15");

    public record VatResult(BigDecimal vatAmount, BigDecimal totalInclVat) {
    }

    /**
     * @param zeroRated the doc states VAT is zero-rated for SARS-classified exports, but there is
     *                   currently no field anywhere in {@code RateComputeRequest} indicating export
     *                   classification — this parameter exists so the calculation logic is ready
     *                   once a real input source exists, but every current call site hardcodes
     *                   {@code false} (see {@code TotalsComputationService}). Tracked as the
     *                   {@code vat_zero_rating_deferred} project memory: zero-rating can never
     *                   actually trigger in practice today.
     */
    public VatResult computeVat(BigDecimal subtotal, boolean zeroRated) {
        if (zeroRated) {
            return new VatResult(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), subtotal.setScale(2, RoundingMode.HALF_UP));
        }
        BigDecimal vatAmount = subtotal.multiply(VAT_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalInclVat = subtotal.setScale(2, RoundingMode.HALF_UP).add(vatAmount);
        return new VatResult(vatAmount, totalInclVat);
    }
}
