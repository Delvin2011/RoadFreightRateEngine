package com.vantageit.road_freight_rate_engine.rateengine.api;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.RateComputeRequest;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.RateComputeResponse;
import com.vantageit.road_freight_rate_engine.rateengine.orchestration.PipelineOrchestrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code POST /api/v1/rate-engine/compute} — the whole pipeline (Stages 1-10) behind one call.
 * No {@code @Valid} here: unlike {@code items}, this request's business-rule validation is Stage
 * 1's own job (already run inside {@link PipelineOrchestrationService#compute}), not JSR-380
 * annotations on the DTO.
 */
@RestController
@RequestMapping("/api/v1/rate-engine")
public class RateEngineController {

    private final PipelineOrchestrationService pipelineOrchestrationService;

    public RateEngineController(PipelineOrchestrationService pipelineOrchestrationService) {
        this.pipelineOrchestrationService = pipelineOrchestrationService;
    }

    @PostMapping("/compute")
    public ResponseEntity<RateComputeResponse> compute(@RequestBody RateComputeRequest request) {
        return ResponseEntity.ok(pipelineOrchestrationService.compute(request));
    }
}
