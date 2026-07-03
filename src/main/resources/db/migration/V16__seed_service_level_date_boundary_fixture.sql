-- Test-only fixture for date-boundary inclusivity on SurchargeRateRepository.findActiveSurcharges,
-- the mechanism AccessorialChargeCalculator relies on. Uses a dedicated surcharge_code (not one of
-- the 6 production accessorial codes from V15) since AccessorialChargeCalculator's codes are fixed
-- and adding a second active row for a real code would violate "exactly one active row per code".
INSERT INTO surcharge_rates
    (id, surcharge_code, surcharge_type, rate_value, applies_to_vehicle_categories, applies_to_cargo_classes, applies_to_route_types, effective_from, effective_to, is_active, created_by, created_at)
VALUES
    ('50000000-0000-0000-0000-000000000009', N'SL_TEST_DATE_BOUNDARY', N'flat', 100.0000, NULL, NULL, N'both', '2025-06-01', '2025-06-30', 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00');
