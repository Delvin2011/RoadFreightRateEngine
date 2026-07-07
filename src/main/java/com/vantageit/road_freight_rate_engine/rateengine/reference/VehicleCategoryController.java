package com.vantageit.road_freight_rate_engine.rateengine.reference;

import com.vantageit.road_freight_rate_engine.rateengine.reference.dto.VehicleCategoryRequest;
import com.vantageit.road_freight_rate_engine.rateengine.reference.dto.VehicleCategoryResponse;
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
@RequestMapping("/vehicle-categories")
public class VehicleCategoryController {

    private final VehicleCategoryService vehicleCategoryService;

    public VehicleCategoryController(VehicleCategoryService vehicleCategoryService) {
        this.vehicleCategoryService = vehicleCategoryService;
    }

    @PostMapping
    public ResponseEntity<VehicleCategoryResponse> create(@Valid @RequestBody VehicleCategoryRequest request) {
        VehicleCategoryResponse created = vehicleCategoryService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public ResponseEntity<List<VehicleCategoryResponse>> getAll() {
        return ResponseEntity.ok(vehicleCategoryService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleCategoryResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(vehicleCategoryService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VehicleCategoryResponse> update(@PathVariable UUID id, @Valid @RequestBody VehicleCategoryRequest request) {
        return ResponseEntity.ok(vehicleCategoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        vehicleCategoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
