package com.vantageit.road_freight_rate_engine.rateengine.rates;

import com.vantageit.road_freight_rate_engine.rateengine.rates.dto.ExpireRequest;
import com.vantageit.road_freight_rate_engine.rateengine.rates.dto.SurchargeRateRequest;
import com.vantageit.road_freight_rate_engine.rateengine.rates.dto.SurchargeRateResponse;
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

/** No PUT — same "expire, don't mutate" reasoning as {@code RoadFreightRateController}. */
@RestController
@RequestMapping("/surcharge-rates")
public class SurchargeRateController {

    private final SurchargeRateService surchargeRateService;

    public SurchargeRateController(SurchargeRateService surchargeRateService) {
        this.surchargeRateService = surchargeRateService;
    }

    @PostMapping
    public ResponseEntity<SurchargeRateResponse> create(@Valid @RequestBody SurchargeRateRequest request) {
        SurchargeRateResponse created = surchargeRateService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public ResponseEntity<List<SurchargeRateResponse>> getAll() {
        return ResponseEntity.ok(surchargeRateService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SurchargeRateResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(surchargeRateService.getById(id));
    }

    @PatchMapping("/{id}/expire")
    public ResponseEntity<SurchargeRateResponse> expire(@PathVariable UUID id, @Valid @RequestBody ExpireRequest request) {
        return ResponseEntity.ok(surchargeRateService.expire(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        surchargeRateService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
