-- Test-only fixtures for BaseFreightComputationServiceTest. lane_key has no FK to zones, so
-- each scenario uses its own synthetic lane_key string for full isolation from real lanes and
-- from every other stage's fixtures — LaneResolutionResult/VehicleSelectionResult are constructed
-- directly in the test (this stage doesn't re-run Stage 4/5), so no zones/locations/
-- lane_distances/vehicle_category_load_types rows are needed, only road_freight_rates itself.

INSERT INTO vehicle_categories (id, code, name, max_weight_kg, max_volume_cbm, description, zone_restriction, requires_permit) VALUES
    ('30000000-0000-0000-0000-000000000011', N'BF_TEST_VEHICLE', N'Base Freight Test Vehicle', 20000.00, 80.00, N'Dedicated fixture for BaseFreightComputationServiceTest, not used by any other stage''s tests', NULL, 0);

INSERT INTO road_freight_rates
    (id, lane_key, vehicle_category_id, load_type, rate_basis, rate_value, currency, minimum_charge, maximum_weight_kg, carrier_id, effective_from, effective_to, is_active, created_by, created_at, version_tag)
VALUES
    -- Per-km, no flooring: raw = 10.0000 * 100.00km * 1000kg / 1000 = 1000.0000, min_charge=500.00 doesn't floor it.
    ('40000000-0000-0000-0000-000000000012', N'BF_TEST_PERKM_NOFLOOR', '30000000-0000-0000-0000-000000000011', N'ftl', N'per_km', 10.0000, 'ZAR', 500.00, NULL, NULL, '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00', N'v1'),
    -- Per-km, flooring applies: same raw (1000.0000), but min_charge=5000.00 floors it.
    ('40000000-0000-0000-0000-000000000013', N'BF_TEST_PERKM_FLOOR', '30000000-0000-0000-0000-000000000011', N'ftl', N'per_km', 10.0000, 'ZAR', 5000.00, NULL, NULL, '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00', N'v1'),
    -- Flat: BASE_FREIGHT is the flat_rate_zar value as-is. Currency deliberately USD (not ZAR)
    -- here, to prove currency is genuinely carried through from the matched row, not hardcoded.
    ('40000000-0000-0000-0000-000000000014', N'BF_TEST_FLAT', '30000000-0000-0000-0000-000000000011', N'ftl', N'flat', 8000.0000, 'USD', NULL, NULL, NULL, '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00', N'v1'),
    -- Flat-vs-per-km precedence on the same FTL lane: flat (6000.00) must win over per_km (which
    -- would otherwise compute a much larger value at any nontrivial distance/weight).
    ('40000000-0000-0000-0000-000000000015', N'BF_TEST_PRECEDENCE', '30000000-0000-0000-0000-000000000011', N'ftl', N'flat', 6000.0000, 'ZAR', NULL, NULL, NULL, '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00', N'v1'),
    ('40000000-0000-0000-0000-000000000016', N'BF_TEST_PRECEDENCE', '30000000-0000-0000-0000-000000000011', N'ftl', N'per_km', 99.0000, 'ZAR', NULL, NULL, NULL, '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00', N'v1'),
    -- Ambiguous: two conflicting rows on a non-FTL load type (reefer) — the flat-vs-per-km FTL
    -- exception does not apply here, so this must be rejected, not silently resolved.
    ('40000000-0000-0000-0000-000000000017', N'BF_TEST_AMBIGUOUS', '30000000-0000-0000-0000-000000000011', N'reefer', N'per_ton', 5.0000, 'ZAR', NULL, NULL, NULL, '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00', N'v1'),
    ('40000000-0000-0000-0000-000000000018', N'BF_TEST_AMBIGUOUS', '30000000-0000-0000-0000-000000000011', N'reefer', N'per_cbm', 3.0000, 'ZAR', NULL, NULL, NULL, '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00', N'v1'),
    -- LTL, no flooring: raw = 500.0000 * 5 pallets = 2500.00, min_charge=1000.00 doesn't floor it.
    ('40000000-0000-0000-0000-000000000019', N'BF_TEST_LTL_NOFLOOR', '30000000-0000-0000-0000-000000000011', N'ltl', N'per_pallet', 500.0000, 'ZAR', 1000.00, NULL, NULL, '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00', N'v1'),
    -- LTL, flooring applies: raw = 100.0000 * 3 pallets = 300.00, min_charge=5000.00 floors it.
    ('40000000-0000-0000-0000-000000000020', N'BF_TEST_LTL_FLOOR', '30000000-0000-0000-0000-000000000011', N'ltl', N'per_pallet', 100.0000, 'ZAR', 5000.00, NULL, NULL, '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00', N'v1'),
    -- Per-ton: raw = 200.0000 * 5000kg / 1000 = 1000.0000, min_charge=50.00 doesn't floor it.
    ('40000000-0000-0000-0000-000000000021', N'BF_TEST_PERTON', '30000000-0000-0000-0000-000000000011', N'bulk_tipper', N'per_ton', 200.0000, 'ZAR', 50.00, NULL, NULL, '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00', N'v1');
