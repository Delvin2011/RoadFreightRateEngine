package com.vantageit.road_freight_rate_engine.rateengine.rates;

import com.vantageit.road_freight_rate_engine.common.exception.ResourceNotFoundException;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.RouteApplicability;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.SurchargeRate;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.SurchargeType;
import com.vantageit.road_freight_rate_engine.rateengine.domain.repository.SurchargeRateRepository;
import com.vantageit.road_freight_rate_engine.rateengine.rates.dto.ExpireRequest;
import com.vantageit.road_freight_rate_engine.rateengine.rates.dto.SurchargeRateRequest;
import com.vantageit.road_freight_rate_engine.rateengine.rates.dto.SurchargeRateResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SurchargeRateService {

    private final SurchargeRateRepository surchargeRateRepository;

    public SurchargeRateService(SurchargeRateRepository surchargeRateRepository) {
        this.surchargeRateRepository = surchargeRateRepository;
    }

    public SurchargeRateResponse create(SurchargeRateRequest request) {
        SurchargeRate rate = SurchargeRate.builder()
                .surchargeCode(request.surchargeCode())
                .surchargeType(SurchargeType.fromWireValue(request.surchargeType()))
                .rateValue(request.rateValue())
                .currency(request.currency())
                .appliesToVehicleCategories(request.appliesToVehicleCategories())
                .appliesToCargoClasses(request.appliesToCargoClasses())
                .appliesToRouteTypes(RouteApplicability.fromWireValue(request.appliesToRouteTypes()))
                .effectiveFrom(request.effectiveFrom())
                .active(true)
                .createdBy(request.createdBy())
                .createdAt(Instant.now())
                .build();
        return toResponse(surchargeRateRepository.save(rate));
    }

    @Transactional(readOnly = true)
    public List<SurchargeRateResponse> getAll() {
        return surchargeRateRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public SurchargeRateResponse getById(UUID id) {
        return toResponse(findOrThrow(id));
    }

    public SurchargeRateResponse expire(UUID id, ExpireRequest request) {
        SurchargeRate rate = findOrThrow(id);
        rate.expire(request.expiryDate());
        return toResponse(surchargeRateRepository.save(rate));
    }

    public void delete(UUID id) {
        surchargeRateRepository.delete(findOrThrow(id));
    }

    private SurchargeRate findOrThrow(UUID id) {
        return surchargeRateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SurchargeRate", id));
    }

    private SurchargeRateResponse toResponse(SurchargeRate rate) {
        return new SurchargeRateResponse(
                rate.getId(),
                rate.getSurchargeCode(),
                rate.getSurchargeType().getWireValue(),
                rate.getRateValue(),
                rate.getCurrency(),
                rate.getAppliesToVehicleCategories(),
                rate.getAppliesToCargoClasses(),
                rate.getAppliesToRouteTypes().getWireValue(),
                rate.getEffectiveFrom(),
                rate.getEffectiveTo(),
                rate.isActive(),
                rate.getCreatedBy(),
                rate.getCreatedAt());
    }
}
