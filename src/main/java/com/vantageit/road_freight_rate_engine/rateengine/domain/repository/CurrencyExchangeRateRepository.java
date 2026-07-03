package com.vantageit.road_freight_rate_engine.rateengine.domain.repository;

import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.CurrencyExchangeRate;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CurrencyExchangeRateRepository extends JpaRepository<CurrencyExchangeRate, UUID> {

    /**
     * Rate lookup policy: the rate with the most recent {@code rate_date} on or before
     * {@code asOfDate} — not an exact-date match, since rates aren't published every single day
     * (weekends/holidays).
     *
     * <p>Deliberately not a {@code findFirstBy...OrderBy...} derived query with an implicit LIMIT:
     * that form compiles to a SQL Server {@code FETCH FIRST ... ROWS ONLY} clause via this
     * Hibernate/dialect combination, which real SQL Server rejects ("Invalid usage of the option
     * first in the FETCH statement" — it requires a paired {@code OFFSET} clause, which Hibernate
     * doesn't emit for a bare LIMIT-1 here). H2 accepts it; SQL Server doesn't — confirmed the hard
     * way, same category as the boolean-literal and multi-column-ALTER divergences already
     * documented in this codebase. Fetching the ordered candidate list and taking the first
     * element in Java sidesteps the dialect issue entirely; the row count per currency pair is
     * expected to stay small (one row per publish date), so this isn't a real performance concern.
     */
    default Optional<CurrencyExchangeRate> findMostRecentRateOnOrBefore(String fromCurrency, String toCurrency, LocalDate asOfDate) {
        return findCandidateRatesOrderedByMostRecent(fromCurrency, toCurrency, asOfDate).stream().findFirst();
    }

    @Query("""
            SELECT r FROM CurrencyExchangeRate r
            WHERE r.fromCurrency = :fromCurrency
              AND r.toCurrency = :toCurrency
              AND r.rateDate <= :asOfDate
            ORDER BY r.rateDate DESC
            """)
    List<CurrencyExchangeRate> findCandidateRatesOrderedByMostRecent(
            @Param("fromCurrency") String fromCurrency,
            @Param("toCurrency") String toCurrency,
            @Param("asOfDate") LocalDate asOfDate);
}
