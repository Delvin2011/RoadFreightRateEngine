-- Additional test-only fixtures for BaseFreightComputationServiceTest's follow-up review cases.
-- Same isolation approach as V13: synthetic lane_key values, no zones/locations/lane_distances/
-- vehicle_category_load_types rows needed.

INSERT INTO vehicle_categories (id, code, name, max_weight_kg, max_volume_cbm, description, zone_restriction, requires_permit) VALUES
    ('30000000-0000-0000-0000-000000000012', N'BF_TEST_VEHICLE_2', N'Base Freight Test Vehicle 2', 20000.00, 80.00, N'Second dedicated fixture, used only to prove vehicle-category scoping in RateRowResolver', NULL, 0);

INSERT INTO road_freight_rates
    (id, lane_key, vehicle_category_id, load_type, rate_basis, rate_value, currency, minimum_charge, maximum_weight_kg, carrier_id, effective_from, effective_to, is_active, created_by, created_at, version_tag)
VALUES
    -- Exact-equality boundary: raw = 10.0000 * 100.00 * 1000 / 1000 = 1000.0000, minimum_charge is
    -- exactly 1000.00 too. compareTo == 0 is not "< 0", so this must NOT floor.
    ('40000000-0000-0000-0000-000000000022', N'BF_TEST_PERKM_EXACT_MIN', '30000000-0000-0000-0000-000000000011', N'ftl', N'per_km', 10.0000, 'ZAR', 1000.00, NULL, NULL, '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00', N'v1'),
    -- minimum_charge is genuinely NULL (not just a low value that happens not to floor).
    ('40000000-0000-0000-0000-000000000023', N'BF_TEST_PERKM_NULL_MIN', '30000000-0000-0000-0000-000000000011', N'ftl', N'per_km', 10.0000, 'ZAR', NULL, NULL, NULL, '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00', N'v1'),
    -- Non-ZAR currency (EUR, distinct from the FLAT test's USD) on a PER_KM row.
    ('40000000-0000-0000-0000-000000000024', N'BF_TEST_PERKM_EUR', '30000000-0000-0000-0000-000000000011', N'ftl', N'per_km', 10.0000, 'EUR', NULL, NULL, NULL, '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00', N'v1'),
    -- Flat-vs-per-km conflict on a NON-FTL load type (reefer) — proves the flat-wins precedence
    -- rule is scoped to FTL specifically, not applied to any flat+per_km pair regardless of load type.
    ('40000000-0000-0000-0000-000000000025', N'BF_TEST_FLAT_PERKM_NON_FTL', '30000000-0000-0000-0000-000000000011', N'reefer', N'flat', 4000.0000, 'ZAR', NULL, NULL, NULL, '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00', N'v1'),
    ('40000000-0000-0000-0000-000000000026', N'BF_TEST_FLAT_PERKM_NON_FTL', '30000000-0000-0000-0000-000000000011', N'reefer', N'per_km', 50.0000, 'ZAR', NULL, NULL, NULL, '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00', N'v1'),
    -- Inactive-only: the only row for this lane/vehicle/load_type is is_active=0, so it must not
    -- be returned at all (RateNotFoundException, not a silently-returned inactive row).
    ('40000000-0000-0000-0000-000000000027', N'BF_TEST_INACTIVE_ONLY', '30000000-0000-0000-0000-000000000011', N'ftl', N'per_km', 10.0000, 'ZAR', NULL, NULL, NULL, '2025-01-01', NULL, 0, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00', N'v1'),
    -- Date-bounded: effective_from/effective_to are both set (not open-ended), to test inclusive
    -- boundary behavior at exactly those two dates.
    ('40000000-0000-0000-0000-000000000028', N'BF_TEST_DATE_BOUNDARY', '30000000-0000-0000-0000-000000000011', N'ftl', N'per_km', 10.0000, 'ZAR', NULL, NULL, NULL, '2025-06-01', '2025-06-30', 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00', N'v1'),
    -- Vehicle-category scoping: same lane_key + load_type, but two different vehicle_category_id
    -- values, each with its own distinct rate. Resolving for one vehicle must not see the other's row.
    ('40000000-0000-0000-0000-000000000029', N'BF_TEST_VEHICLE_SCOPING', '30000000-0000-0000-0000-000000000011', N'ftl', N'per_km', 10.0000, 'ZAR', NULL, NULL, NULL, '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00', N'v1'),
    ('40000000-0000-0000-0000-000000000030', N'BF_TEST_VEHICLE_SCOPING', '30000000-0000-0000-0000-000000000012', N'ftl', N'per_km', 99.0000, 'ZAR', NULL, NULL, NULL, '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00', N'v1');
