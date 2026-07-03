package com.vantageit.road_freight_rate_engine.rateengine.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * @param commodityCode        HS (Harmonized System) code
 * @param hazmatUnNumber       UN number identifying the dangerous good, only set when {@code cargoClass} is
 *                             {@link CargoClass#HAZMAT}
 * @param hazmatPackingGroup   only set when {@code cargoClass} is {@link CargoClass#HAZMAT}
 * @param imdgClass            IMDG dangerous goods class, only set when {@code cargoClass} is {@link CargoClass#HAZMAT}
 * @param declaredValueZar     only set when {@code highValueDeclared} is true
 */
public record CargoRequest(
        @JsonProperty("cargo_class") CargoClass cargoClass,
        @JsonProperty("commodity_code") String commodityCode,
        @JsonProperty("gross_weight_kg") BigDecimal grossWeightKg,
        @JsonProperty("volume_cbm") BigDecimal volumeCbm,
        @JsonProperty("load_type") LoadType loadType,
        @JsonProperty("pallet_count") Integer palletCount,
        @JsonProperty("container_type") ContainerType containerType,
        @JsonProperty("package_type") PackageType packageType,
        Boolean stackable,
        @JsonProperty("dimensions_lxwxh_m") Dimensions dimensionsLxwxhM,
        @JsonProperty("temperature_range_c") TemperatureRange temperatureRangeC,
        @JsonProperty("hazmat_un_number") String hazmatUnNumber,
        @JsonProperty("hazmat_packing_group") PackingGroup hazmatPackingGroup,
        @JsonProperty("imdg_class") String imdgClass,
        @JsonProperty("high_value_declared") Boolean highValueDeclared,
        @JsonProperty("declared_value_zar") BigDecimal declaredValueZar,
        @JsonProperty("security_escort_required") Boolean securityEscortRequired,
        @JsonProperty("abnormal_load") Boolean abnormalLoad,
        @JsonProperty("live_animals") Boolean liveAnimals,
        @JsonProperty("project_cargo") Boolean projectCargo
) {
}
