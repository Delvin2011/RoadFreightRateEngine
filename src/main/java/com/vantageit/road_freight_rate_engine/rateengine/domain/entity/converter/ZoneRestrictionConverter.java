package com.vantageit.road_freight_rate_engine.rateengine.domain.entity.converter;

import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.ZoneRestriction;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ZoneRestrictionConverter implements AttributeConverter<ZoneRestriction, String> {

    @Override
    public String convertToDatabaseColumn(ZoneRestriction attribute) {
        return attribute == null ? null : attribute.getWireValue();
    }

    @Override
    public ZoneRestriction convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ZoneRestriction.fromWireValue(dbData);
    }
}
