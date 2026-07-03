package com.vantageit.road_freight_rate_engine.rateengine.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
 * Immutable once activated: {@code rateValue}, {@code effectiveFrom} and every other priced/dated
 * field are constructor-only. A rate change is always a new row. Only {@link #expire(LocalDate)}
 * may mutate an existing row, to supersede it when a new rate takes effect.
 */
@Entity
@Table(name = "road_freight_rates")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RoadFreightRate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "lane_key", nullable = false, length = 100, updatable = false)
    private String laneKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_category_id", nullable = false, updatable = false)
    private VehicleCategory vehicleCategory;

    @Column(name = "load_type", nullable = false, length = 20, updatable = false)
    private String loadType;

    @Column(name = "rate_basis", nullable = false, length = 20, updatable = false)
    private RateBasis rateBasis;

    @Column(name = "rate_value", nullable = false, precision = 12, scale = 4, updatable = false)
    private BigDecimal rateValue;

    @Column(name = "currency", nullable = false, length = 3, updatable = false)
    private String currency;

    @Column(name = "minimum_charge", precision = 12, scale = 2, updatable = false)
    private BigDecimal minimumCharge;

    @Column(name = "maximum_weight_kg", precision = 12, scale = 2, updatable = false)
    private BigDecimal maximumWeightKg;

    /** Null means internal fleet. */
    @Column(name = "carrier_id", updatable = false)
    private UUID carrierId;

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

    @Column(name = "version_tag", length = 50, updatable = false)
    private String versionTag;

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
        if (!(o instanceof RoadFreightRate other)) {
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
        return "RoadFreightRate{id=%s, laneKey=%s, loadType=%s, rateBasis=%s, rateValue=%s, currency=%s, effectiveFrom=%s, effectiveTo=%s, active=%s}"
                .formatted(id, laneKey, loadType, rateBasis, rateValue, currency, effectiveFrom, effectiveTo, active);
    }
}
