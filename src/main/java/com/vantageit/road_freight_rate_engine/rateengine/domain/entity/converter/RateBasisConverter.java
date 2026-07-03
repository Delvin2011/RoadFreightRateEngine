package com.vantageit.road_freight_rate_engine.rateengine.domain.entity.converter;

import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.RateBasis;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RateBasisConverter implements AttributeConverter<RateBasis, String> {

    @Override
    public String convertToDatabaseColumn(RateBasis attribute) {
        return attribute == null ? null : attribute.getWireValue();
    }

    @Override
    public RateBasis convertToEntityAttribute(String dbData) {
        return dbData == null ? null : RateBasis.fromWireValue(dbData);
    }
}
