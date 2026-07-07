package com.vantageit.road_freight_rate_engine.rateengine.rates.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Creates a new exchange rate row. No update/expire endpoint exists for this entity — unlike
 * {@link RoadFreightRateRequest}/{@link SurchargeRateRequest}, {@code CurrencyExchangeRate} has no
 * {@code active}/{@code effectiveTo} field at all; a later {@code rateDate} simply wins on lookup.
 */
public record CurrencyExchangeRateRequest(

        @NotBlank(message = "From-currency is required")
        @Size(max = 3, message = "From-currency must be at most 3 characters")
        String fromCurrency,

        @NotBlank(message = "To-currency is required")
        @Size(max = 3, message = "To-currency must be at most 3 characters")
        String toCurrency,

        @NotNull(message = "Rate is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Rate must be greater than zero")
        BigDecimal rate,

        @NotNull(message = "Rate date is required")
        LocalDate rateDate,

        @Size(max = 100, message = "Source must be at most 100 characters")
        String source
) {
}
