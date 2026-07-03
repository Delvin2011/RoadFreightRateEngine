package com.vantageit.road_freight_rate_engine.rateengine.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Immutable, same pattern as {@link RoadFreightRate}/{@link SurchargeRate}: every field is
 * constructor-only, no setters — a new rate is a new row, never a mutation of an existing one.
 */
@Entity
@Table(name = "currency_exchange_rates")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CurrencyExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "from_currency", nullable = false, length = 3, updatable = false)
    private String fromCurrency;

    /** Phase 1 always 'ZAR' — modeled as a real column rather than hardcoded, see V20's comment. */
    @Column(name = "to_currency", nullable = false, length = 3, updatable = false)
    private String toCurrency;

    @Column(name = "rate", nullable = false, precision = 18, scale = 6, updatable = false)
    private BigDecimal rate;

    @Column(name = "rate_date", nullable = false, updatable = false)
    private LocalDate rateDate;

    @Column(name = "source", length = 100, updatable = false)
    private String source;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CurrencyExchangeRate other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "CurrencyExchangeRate{id=%s, fromCurrency=%s, toCurrency=%s, rate=%s, rateDate=%s}"
                .formatted(id, fromCurrency, toCurrency, rate, rateDate);
    }
}
