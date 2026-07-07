package com.vantageit.road_freight_rate_engine.rateengine.reference;

import com.vantageit.road_freight_rate_engine.rateengine.reference.dto.LaneDistanceRequest;
import com.vantageit.road_freight_rate_engine.rateengine.reference.dto.LaneDistanceResponse;
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
@RequestMapping("/lane-distances")
public class LaneDistanceController {

    private final LaneDistanceService laneDistanceService;

    public LaneDistanceController(LaneDistanceService laneDistanceService) {
        this.laneDistanceService = laneDistanceService;
    }

    @PostMapping
    public ResponseEntity<LaneDistanceResponse> create(@Valid @RequestBody LaneDistanceRequest request) {
        LaneDistanceResponse created = laneDistanceService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public ResponseEntity<List<LaneDistanceResponse>> getAll() {
        return ResponseEntity.ok(laneDistanceService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LaneDistanceResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(laneDistanceService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LaneDistanceResponse> update(@PathVariable UUID id, @Valid @RequestBody LaneDistanceRequest request) {
        return ResponseEntity.ok(laneDistanceService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        laneDistanceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
