package com.vantageit.road_freight_rate_engine.items;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vantageit.road_freight_rate_engine.items.dto.ItemRequest;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ItemControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createReadUpdateDeleteItem() throws Exception {
        String initialListResponse = mockMvc.perform(get("/items"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        int initialCount = objectMapper.readTree(initialListResponse).size();

        ItemRequest createRequest = new ItemRequest("Tarp Strap", "Ratchet strap for tarps", 20, new BigDecimal("15.00"));

        String createResponse = mockMvc.perform(post("/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Tarp Strap"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(get("/items/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tarp Strap"));

        String listResponse = mockMvc.perform(get("/items"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(objectMapper.readTree(listResponse)).hasSize(initialCount + 1);

        ItemRequest updateRequest = new ItemRequest("Tarp Strap XL", "Extra-long ratchet strap", 25, new BigDecimal("18.50"));

        mockMvc.perform(put("/items/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tarp Strap XL"))
                .andExpect(jsonPath("$.quantity").value(25));

        mockMvc.perform(delete("/items/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/items/{id}", id))
                .andExpect(status().isNotFound());
    }
}
