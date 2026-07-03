package com.vantageit.road_freight_rate_engine.items.mapper;

import com.vantageit.road_freight_rate_engine.items.Item;
import com.vantageit.road_freight_rate_engine.items.dto.ItemRequest;
import com.vantageit.road_freight_rate_engine.items.dto.ItemResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface ItemMapper {

    Item toEntity(ItemRequest request);

    ItemResponse toResponse(Item item);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(ItemRequest request, @MappingTarget Item item);
}
