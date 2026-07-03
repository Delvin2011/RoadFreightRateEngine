package com.vantageit.road_freight_rate_engine.items;

import com.vantageit.road_freight_rate_engine.items.dto.ItemRequest;
import com.vantageit.road_freight_rate_engine.items.dto.ItemResponse;
import com.vantageit.road_freight_rate_engine.items.mapper.ItemMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ItemService {

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;

    public ItemService(ItemRepository itemRepository, ItemMapper itemMapper) {
        this.itemRepository = itemRepository;
        this.itemMapper = itemMapper;
    }

    public ItemResponse create(ItemRequest request) {
        Item item = itemMapper.toEntity(request);
        Item saved = itemRepository.save(item);
        return itemMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ItemResponse> getAll() {
        return itemRepository.findAll().stream()
                .map(itemMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ItemResponse getById(Long id) {
        return itemMapper.toResponse(findItemOrThrow(id));
    }

    public ItemResponse update(Long id, ItemRequest request) {
        Item item = findItemOrThrow(id);
        itemMapper.updateEntityFromRequest(request, item);
        Item saved = itemRepository.save(item);
        return itemMapper.toResponse(saved);
    }

    public void delete(Long id) {
        itemRepository.delete(findItemOrThrow(id));
    }

    private Item findItemOrThrow(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException(id));
    }
}
