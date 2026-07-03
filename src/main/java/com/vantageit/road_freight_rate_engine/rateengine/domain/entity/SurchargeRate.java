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
 * Immutable once activated, same pattern as {@link RoadFreightRate}: {@code rateValue} and every
 * other priced/dated field are constructor-only, and only {@link #expire(LocalDate)} may mutate
 * an existing row.
 */
@Entity
@Table(name = "surcharge_rates")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SurchargeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "surcharge_code", nullable = false, length = 50, updatable = false)
    private String surchargeCode;

    @Column(name = "surcharge_type", nullable = false, length = 20, updatable = false)
    private SurchargeType surchargeType;

    /** Percentage as decimal (0.2200 = 22%) when surchargeType is PCT_OF_BASE; a flat/unit amount otherwise. */
    @Column(name = "rate_value", nullable = false, precision = 10, scale = 4, updatable = false)
    private BigDecimal rateValue;

    @Column(name = "currency", nullable = false, length = 3, updatable = false)
    private String currency;

    /** Comma-delimited vehicle_categories.code list. Null means all vehicle categories. */
    @Column(name = "applies_to_vehicle_categories", updatable = false)
    private String appliesToVehicleCategories;

    /** Comma-delimited Stage 1 CargoClass wire values. Null means all cargo classes. */
    @Column(name = "applies_to_cargo_classes", updatable = false)
    private String appliesToCargoClasses;

    @Column(name = "applies_to_route_types", nullable = false, length = 20, updatable = false)
    private RouteApplicability appliesToRouteTypes;

    @Column(name = "effective_from", nullable = false, updatable = false)
    private LocalDate effectiveFrom;

    /** Null means open-ended (this is still the current rate). Mutated only by {@link #expire}. */
    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Supersedes this row: closes it off as of {@code expiryDate} and marks it inactive. */
    public void expire(LocalDate expiryDate) {
        this.effectiveTo = expiryDate;
        this.active = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SurchargeRate other)) {
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
        return "SurchargeRate{id=%s, surchargeCode=%s, surchargeType=%s, rateValue=%s, currency=%s, appliesToRouteTypes=%s, effectiveFrom=%s, effectiveTo=%s, active=%s}"
                .formatted(id, surchargeCode, surchargeType, rateValue, currency, appliesToRouteTypes, effectiveFrom, effectiveTo, active);
    }
}
