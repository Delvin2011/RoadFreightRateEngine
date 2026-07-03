package com.vantageit.road_freight_rate_engine.rateengine.domain.repository;

import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.VehicleCategory;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.VehicleCategoryLoadType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VehicleCategoryLoadTypeRepository extends JpaRepository<VehicleCategoryLoadType, UUID> {

    @Query("SELECT vclt.vehicleCategory FROM VehicleCategoryLoadType vclt WHERE vclt.loadType = :loadType")
    List<VehicleCategory> findVehicleCategoriesByLoadType(@Param("loadType") String loadType);
}
