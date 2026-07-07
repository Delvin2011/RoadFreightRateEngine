package com.vantageit.road_freight_rate_engine.rateengine.reference;

import com.vantageit.road_freight_rate_engine.rateengine.reference.dto.BorderPostRequest;
import com.vantageit.road_freight_rate_engine.rateengine.reference.dto.BorderPostResponse;
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
@RequestMapping("/border-posts")
public class BorderPostController {

    private final BorderPostService borderPostService;

    public BorderPostController(BorderPostService borderPostService) {
        this.borderPostService = borderPostService;
    }

    @PostMapping
    public ResponseEntity<BorderPostResponse> create(@Valid @RequestBody BorderPostRequest request) {
        BorderPostResponse created = borderPostService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public ResponseEntity<List<BorderPostResponse>> getAll() {
        return ResponseEntity.ok(borderPostService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BorderPostResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(borderPostService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BorderPostResponse> update(@PathVariable UUID id, @Valid @RequestBody BorderPostRequest request) {
        return ResponseEntity.ok(borderPostService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        borderPostService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
