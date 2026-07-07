package com.vantageit.road_freight_rate_engine.rateengine.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vehicle_categories")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class VehicleCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "max_weight_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal maxWeightKg;

    @Column(name = "max_volume_cbm", nullable = false, precision = 10, scale = 2)
    private BigDecimal maxVolumeCbm;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    /** Null means no restriction. */
    @Column(name = "zone_restriction", length = 30)
    private ZoneRestriction zoneRestriction;

    @Column(name = "requires_permit", nullable = false)
    private boolean requiresPermit;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VehicleCategory other)) {
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
        return "VehicleCategory{id=%s, code=%s, name=%s, maxWeightKg=%s, maxVolumeCbm=%s}"
                .formatted(id, code, name, maxWeightKg, maxVolumeCbm);
    }
}
