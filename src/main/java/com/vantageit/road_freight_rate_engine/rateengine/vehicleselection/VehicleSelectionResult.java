package com.vantageit.road_freight_rate_engine.rateengine.vehicleselection;

import java.util.UUID;

/**
 * @param requiresPermit       the selected vehicle's own {@code requires_permit} flag, carried
 *                             here explicitly so a later permit-surcharge pipeline stage doesn't
 *                             need to re-query {@code vehicle_categories} for it
 * @param eligibleVehicleCount for observability/debugging — how many vehicles passed Phase 1
 */
public record VehicleSelectionResult(
        UUID selectedVehicleCategoryId,
        String selectedVehicleCategoryCode,
        SelectionReason selectionReason,
        boolean requiresPermit,
        int eligibleVehicleCount
) {
}
