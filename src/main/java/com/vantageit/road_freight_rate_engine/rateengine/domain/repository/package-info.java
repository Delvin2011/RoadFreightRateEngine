/**
 * Spring Data JPA repositories for the rate engine domain entities.
 *
 * <p><b>Boolean predicates must be bound as JDBC parameters, never inlined.</b> H2's
 * {@code MODE=MSSQLServer} (used by the default test suite) approximates T-SQL syntax, not SQL
 * Server's actual type-coercion rules, and has passed JPQL that real SQL Server rejects:
 * {@code WHERE r.active = true} fails on SQL Server with "Invalid column name 'true'", and a bare
 * {@code WHERE r.active} fails with "An expression of non-boolean type specified in a context
 * where a condition is expected" — a {@code BIT} column can't stand alone as a boolean expression
 * in T-SQL, and {@code true}/{@code false} aren't recognized as {@code BIT} literals.
 *
 * <p>See {@link com.vantageit.road_freight_rate_engine.rateengine.domain.repository.RoadFreightRateRepository}
 * and {@link com.vantageit.road_freight_rate_engine.rateengine.domain.repository.SurchargeRateRepository}
 * for the fix: a {@code default} method exposes the clean public signature while an internal
 * {@code @Query} method binds the boolean as a real {@code @Param}.
 *
 * <p>Any new query method with a boolean/{@code BIT}-column predicate should be run against a real
 * SQL Server instance before it's considered done — passing on H2 alone is not sufficient evidence.
 * {@code RateTablesIntegrationTest} is the reference example of a test run this way; see the
 * project README's "Test" section for the command.
 */
package com.vantageit.road_freight_rate_engine.rateengine.domain.repository;
