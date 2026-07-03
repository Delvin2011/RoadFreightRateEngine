package com.vantageit.road_freight_rate_engine.rateengine.domain.repository;

import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.Location;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository extends JpaRepository<Location, UUID> {
}
