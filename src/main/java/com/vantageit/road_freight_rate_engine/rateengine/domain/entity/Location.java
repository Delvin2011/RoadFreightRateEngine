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
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** What {@code origin_location_id}/{@code destination_location_id} in the API request refer to. */
@Entity
@Table(name = "locations")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", nullable = false, updatable = false)
    private Zone zone;

    @Column(name = "name", nullable = false, length = 255, updatable = false)
    private String name;

    @Column(name = "address", length = 500, updatable = false)
    private String address;

    /** Informational only (e.g. depot, port, inland_depot, client_site) — not used for logic. */
    @Column(name = "location_type", length = 30, updatable = false)
    private String locationType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Location other)) {
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
        return "Location{id=%s, name=%s, locationType=%s}".formatted(id, name, locationType);
    }
}
