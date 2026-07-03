package com.vantageit.road_freight_rate_engine.items;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vantageit.road_freight_rate_engine.items.mapper.ItemMapper;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ItemMapper itemMapper;

    private ItemService itemService;

    @BeforeEach
    void setUp() {
        itemService = new ItemService(itemRepository, itemMapper);
    }

    @Test
    void getByIdThrowsWhenItemDoesNotExist() {
        Long missingId = 99L;
        when(itemRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.getById(missingId))
                .isInstanceOf(ItemNotFoundException.class)
                .hasMessage("Item not found with id: 99");

        verify(itemRepository).findById(missingId);
        verifyNoInteractions(itemMapper);
    }
}
