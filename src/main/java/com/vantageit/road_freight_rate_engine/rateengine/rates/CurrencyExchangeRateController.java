package com.vantageit.road_freight_rate_engine.rateengine.rates;

import com.vantageit.road_freight_rate_engine.rateengine.rates.dto.CurrencyExchangeRateRequest;
import com.vantageit.road_freight_rate_engine.rateengine.rates.dto.CurrencyExchangeRateResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** No PUT and no expire endpoint — the entity itself has no mutable field; a new rate is always a new row. */
@RestController
@RequestMapping("/currency-exchange-rates")
public class CurrencyExchangeRateController {

    private final CurrencyExchangeRateService currencyExchangeRateService;

    public CurrencyExchangeRateController(CurrencyExchangeRateService currencyExchangeRateService) {
        this.currencyExchangeRateService = currencyExchangeRateService;
    }

    @PostMapping
    public ResponseEntity<CurrencyExchangeRateResponse> create(@Valid @RequestBody CurrencyExchangeRateRequest request) {
        CurrencyExchangeRateResponse created = currencyExchangeRateService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public ResponseEntity<List<CurrencyExchangeRateResponse>> getAll() {
        return ResponseEntity.ok(currencyExchangeRateService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CurrencyExchangeRateResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(currencyExchangeRateService.getById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        currencyExchangeRateService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
