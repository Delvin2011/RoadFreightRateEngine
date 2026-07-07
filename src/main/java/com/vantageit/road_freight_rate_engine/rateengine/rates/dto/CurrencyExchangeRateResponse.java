package com.vantageit.road_freight_rate_engine.rateengine.rates.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CurrencyExchangeRateResponse(
        UUID id,
        String fromCurrency,
        String toCurrency,
        BigDecimal rate,
        LocalDate rateDate,
        String source,
        Instant createdAt
) {
}
