package com.vantageit.road_freight_rate_engine.rateengine.domain.entity.converter;

import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.SurchargeType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SurchargeTypeConverter implements AttributeConverter<SurchargeType, String> {

    @Override
    public String convertToDatabaseColumn(SurchargeType attribute) {
        return attribute == null ? null : attribute.getWireValue();
    }

    @Override
    public SurchargeType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : SurchargeType.fromWireValue(dbData);
    }
}
