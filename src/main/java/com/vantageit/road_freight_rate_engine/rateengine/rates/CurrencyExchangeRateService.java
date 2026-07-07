package com.vantageit.road_freight_rate_engine.rateengine.rates;

import com.vantageit.road_freight_rate_engine.common.exception.ResourceNotFoundException;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.CurrencyExchangeRate;
import com.vantageit.road_freight_rate_engine.rateengine.domain.repository.CurrencyExchangeRateRepository;
import com.vantageit.road_freight_rate_engine.rateengine.rates.dto.CurrencyExchangeRateRequest;
import com.vantageit.road_freight_rate_engine.rateengine.rates.dto.CurrencyExchangeRateResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CurrencyExchangeRateService {

    private final CurrencyExchangeRateRepository currencyExchangeRateRepository;

    public CurrencyExchangeRateService(CurrencyExchangeRateRepository currencyExchangeRateRepository) {
        this.currencyExchangeRateRepository = currencyExchangeRateRepository;
    }

    public CurrencyExchangeRateResponse create(CurrencyExchangeRateRequest request) {
        CurrencyExchangeRate rate = CurrencyExchangeRate.builder()
                .fromCurrency(request.fromCurrency())
                .toCurrency(request.toCurrency())
                .rate(request.rate())
                .rateDate(request.rateDate())
                .source(request.source())
                .createdAt(Instant.now())
                .build();
        return toResponse(currencyExchangeRateRepository.save(rate));
    }

    @Transactional(readOnly = true)
    public List<CurrencyExchangeRateResponse> getAll() {
        return currencyExchangeRateRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CurrencyExchangeRateResponse getById(UUID id) {
        return toResponse(findOrThrow(id));
    }

    public void delete(UUID id) {
        currencyExchangeRateRepository.delete(findOrThrow(id));
    }

    private CurrencyExchangeRate findOrThrow(UUID id) {
        return currencyExchangeRateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CurrencyExchangeRate", id));
    }

    private CurrencyExchangeRateResponse toResponse(CurrencyExchangeRate rate) {
        return new CurrencyExchangeRateResponse(
                rate.getId(),
                rate.getFromCurrency(),
                rate.getToCurrency(),
                rate.getRate(),
                rate.getRateDate(),
                rate.getSource(),
                rate.getCreatedAt());
    }
}
