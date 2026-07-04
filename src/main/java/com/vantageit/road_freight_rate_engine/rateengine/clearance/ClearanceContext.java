package com.vantageit.road_freight_rate_engine.rateengine.clearance;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.RateComputeRequest;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.BorderPost;
import com.vantageit.road_freight_rate_engine.rateengine.laneresolution.LaneResolutionResult;

/**
 * Bundles everything {@link ClearanceRule}s need, consuming Stage 4's already-computed output
 * rather than reaching back into its services — same decoupling discipline as every prior stage's
 * context object.
 *
 * @param borderPost the resolved {@code border_posts} row for {@code route.border_post_id},
 *                    resolved once by {@link ClearanceComputationService} rather than by each rule
 *                    independently. Null for domestic routes, and also null if the request-supplied
 *                    {@code border_post_id} doesn't resolve to an actual row (there is no FK
 *                    constraint tying a caller-supplied id to {@code border_posts} the way there is
 *                    for {@code lane_distances.border_post_id}) — country-gated rules simply don't
 *                    trigger in that case rather than this stage inventing a new "unknown border
 *                    post" failure mode beyond what was asked for.
 */
public record ClearanceContext(
        RateComputeRequest request,
        LaneResolutionResult lane,
        BorderPost borderPost
) {
}
