package com.vantageit.road_freight_rate_engine.rateengine.rates;

import com.vantageit.road_freight_rate_engine.rateengine.rates.dto.ExpireRequest;
import com.vantageit.road_freight_rate_engine.rateengine.rates.dto.RoadFreightRateRequest;
import com.vantageit.road_freight_rate_engine.rateengine.rates.dto.RoadFreightRateResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * No PUT: {@link RoadFreightRateService} matches the entity's own documented invariant that a rate
 * change is always a new row, never an in-place mutation. Use POST for a new rate and
 * {@code PATCH /{id}/expire} to close out the one it supersedes.
 */
@RestController
@RequestMapping("/road-freight-rates")
public class RoadFreightRateController {

    private final RoadFreightRateService roadFreightRateService;

    public RoadFreightRateController(RoadFreightRateService roadFreightRateService) {
        this.roadFreightRateService = roadFreightRateService;
    }

    @PostMapping
    public ResponseEntity<RoadFreightRateResponse> create(@Valid @RequestBody RoadFreightRateRequest request) {
        RoadFreightRateResponse created = roadFreightRateService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public ResponseEntity<List<RoadFreightRateResponse>> getAll() {
        return ResponseEntity.ok(roadFreightRateService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoadFreightRateResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(roadFreightRateService.getById(id));
    }

    @PatchMapping("/{id}/expire")
    public ResponseEntity<RoadFreightRateResponse> expire(@PathVariable UUID id, @Valid @RequestBody ExpireRequest request) {
        return ResponseEntity.ok(roadFreightRateService.expire(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        roadFreightRateService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
