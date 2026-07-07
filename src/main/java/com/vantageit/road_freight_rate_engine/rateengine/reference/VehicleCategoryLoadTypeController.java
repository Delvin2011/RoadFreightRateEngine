package com.vantageit.road_freight_rate_engine.rateengine.reference;

import com.vantageit.road_freight_rate_engine.rateengine.reference.dto.VehicleCategoryLoadTypeRequest;
import com.vantageit.road_freight_rate_engine.rateengine.reference.dto.VehicleCategoryLoadTypeResponse;
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
@RequestMapping("/vehicle-category-load-types")
public class VehicleCategoryLoadTypeController {

    private final VehicleCategoryLoadTypeService vehicleCategoryLoadTypeService;

    public VehicleCategoryLoadTypeController(VehicleCategoryLoadTypeService vehicleCategoryLoadTypeService) {
        this.vehicleCategoryLoadTypeService = vehicleCategoryLoadTypeService;
    }

    @PostMapping
    public ResponseEntity<VehicleCategoryLoadTypeResponse> create(@Valid @RequestBody VehicleCategoryLoadTypeRequest request) {
        VehicleCategoryLoadTypeResponse created = vehicleCategoryLoadTypeService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public ResponseEntity<List<VehicleCategoryLoadTypeResponse>> getAll() {
        return ResponseEntity.ok(vehicleCategoryLoadTypeService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleCategoryLoadTypeResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(vehicleCategoryLoadTypeService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VehicleCategoryLoadTypeResponse> update(@PathVariable UUID id, @Valid @RequestBody VehicleCategoryLoadTypeRequest request) {
        return ResponseEntity.ok(vehicleCategoryLoadTypeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        vehicleCategoryLoadTypeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
