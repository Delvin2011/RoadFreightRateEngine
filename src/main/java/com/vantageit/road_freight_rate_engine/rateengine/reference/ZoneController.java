package com.vantageit.road_freight_rate_engine.rateengine.reference;

import com.vantageit.road_freight_rate_engine.rateengine.reference.dto.ZoneRequest;
import com.vantageit.road_freight_rate_engine.rateengine.reference.dto.ZoneResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/zones")
public class ZoneController {

    private final ZoneService zoneService;

    public ZoneController(ZoneService zoneService) {
        this.zoneService = zoneService;
    }

    @PostMapping
    public ResponseEntity<ZoneResponse> create(@Valid @RequestBody ZoneRequest request) {
        ZoneResponse created = zoneService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public ResponseEntity<List<ZoneResponse>> getAll() {
        return ResponseEntity.ok(zoneService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ZoneResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(zoneService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ZoneResponse> update(@PathVariable UUID id, @Valid @RequestBody ZoneRequest request) {
        return ResponseEntity.ok(zoneService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        zoneService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
