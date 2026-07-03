package com.vantageit.road_freight_rate_engine.rateengine.domain.entity.converter;

import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.RouteApplicability;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RouteApplicabilityConverter implements AttributeConverter<RouteApplicability, String> {

    @Override
    public String convertToDatabaseColumn(RouteApplicability attribute) {
        return attribute == null ? null : attribute.getWireValue();
    }

    @Override
    public RouteApplicability convertToEntityAttribute(String dbData) {
        return dbData == null ? null : RouteApplicability.fromWireValue(dbData);
    }
}
