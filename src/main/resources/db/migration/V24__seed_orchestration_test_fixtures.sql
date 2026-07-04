-- Additional test-only fixtures for PipelineOrchestrationServiceTest.
--
-- Phase 1 vehicle eligibility (VehicleSelectionService) is capacity/load_type/zone_restriction
-- based, not lane-scoped -- any new vehicle_categories row is visible to every existing test in
-- the whole suite, including VehicleSelectionServiceTest's own eligibleVehicleCount()/winner
-- assertions on JHB_METRO:BFN_METRO and JHB_METRO:HARARE. Three deliberate isolation choices below
-- avoid all of that:
--   1. 9T_EUR_TEST uses load_type 'flatbed' (unused by every other seeded vehicle/test in this
--      codebase) instead of 'ftl' -- its 9000kg capacity would otherwise be Phase-1-eligible for
--      every existing FTL fixture at a smaller weight too (no capacity value avoids this: it must
--      be >= 8500kg for its own test, which is already above every FTL fixture's weight).
--   2. SMALL_EXPENSIVE_TEST/LARGE_CHEAP_TEST/AMBIGUOUS_TEST_VEHICLE all use small, disjoint
--      capacities (1200/2000/2000) chosen to fall strictly below VehicleSelectionServiceTest's own
--      FTL/reefer chargeable-weight fixtures (1665/1900/3330), so they never inflate its exact
--      eligibleVehicleCount() assertions there.
--   3. SMALL_EXPENSIVE_TEST/LARGE_CHEAP_TEST are rated on LIMPOPO_RURAL:HARARE, not
--      JHB_METRO:HARARE -- even with small capacities, LARGE_CHEAP_TEST's flat 500.00 ZAR rate
--      would otherwise out-cost-efficient VehicleSelectionServiceTest's own JHB_METRO:HARARE
--      winners at 1665/1900kg.
-- vehicle_categories ids start at 0015: 0011/0012 were already taken by V13/V14's BF_TEST_VEHICLE/
-- BF_TEST_VEHICLE_2 (Stage 6 fixtures, unrelated to this stage's own vehicle numbering sequence).

-- New, active lane_distances row for LIMPOPO_RURAL -> HARARE via TEST_BORDER_2 -- the only
-- existing row for this zone pair (V8) is via BEIT_BRIDGE and deliberately inactive.
INSERT INTO lane_distances (id, origin_zone_id, destination_zone_id, border_post_id, distance_km, is_active, created_at) VALUES
    ('70000000-0000-0000-0000-000000000006', '10000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000002', 1400.00, 1, '2025-01-01 00:00:00');

INSERT INTO vehicle_categories (id, code, name, max_weight_kg, max_volume_cbm, description, zone_restriction, requires_permit) VALUES
    ('30000000-0000-0000-0000-000000000015', N'9T_EUR_TEST', N'9-Ton Rigid (EUR-rated test fixture, flatbed)', 9000.00, 40.00, N'Dedicated fixture for the 3-currency (ZAR/USD/EUR) orchestration reconciliation test -- flatbed load_type and LIMPOPO_RURAL:HARARE lane both chosen to avoid Phase-1-eligibility collisions with other tests', NULL, 0),
    -- Deliberately sized so cost-efficient and dedicated-minimum-viable selection diverge:
    -- SMALL_EXPENSIVE_TEST is smaller (wins dedicated_vehicle=true) but pricier (loses cost-efficient).
    ('30000000-0000-0000-0000-000000000016', N'SMALL_EXPENSIVE_TEST', N'Small Expensive Test Vehicle', 1200.00, 10.00, N'Smallest-capacity but priciest of the dedicated-vs-cost-efficient divergence pair', NULL, 0),
    ('30000000-0000-0000-0000-000000000017', N'LARGE_CHEAP_TEST', N'Large Cheap Test Vehicle', 2000.00, 20.00, N'Larger-capacity but cheapest of the dedicated-vs-cost-efficient divergence pair', NULL, 0),
    ('30000000-0000-0000-0000-000000000018', N'AMBIGUOUS_TEST_VEHICLE', N'Ambiguous Rate Test Vehicle', 2000.00, 20.00, N'Dedicated fixture with a genuine per_ton/per_cbm rate conflict for AmbiguousRateConfigurationException', NULL, 0);

INSERT INTO vehicle_category_load_types (id, vehicle_category_id, load_type) VALUES
    ('80000000-0000-0000-0000-000000000013', '30000000-0000-0000-0000-000000000015', N'flatbed'),
    ('80000000-0000-0000-0000-000000000014', '30000000-0000-0000-0000-000000000016', N'ftl'),
    ('80000000-0000-0000-0000-000000000015', '30000000-0000-0000-0000-000000000017', N'ftl'),
    ('80000000-0000-0000-0000-000000000016', '30000000-0000-0000-0000-000000000018', N'reefer');

INSERT INTO road_freight_rates
    (id, lane_key, vehicle_category_id, load_type, rate_basis, rate_value, currency, minimum_charge, maximum_weight_kg, carrier_id, effective_from, effective_to, is_active, created_by, created_at, version_tag)
VALUES
    ('40000000-0000-0000-0000-000000000032', N'LIMPOPO_RURAL:HARARE', '30000000-0000-0000-0000-000000000015', N'flatbed', N'per_km', 8.0000, 'EUR', NULL, NULL, NULL, '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00', N'v1'),
    ('40000000-0000-0000-0000-000000000033', N'LIMPOPO_RURAL:HARARE', '30000000-0000-0000-0000-000000000016', N'ftl', N'flat', 9000.0000, 'ZAR', NULL, NULL, NULL, '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00', N'v1'),
    ('40000000-0000-0000-0000-000000000034', N'LIMPOPO_RURAL:HARARE', '30000000-0000-0000-0000-000000000017', N'ftl', N'flat', 500.0000, 'ZAR', NULL, NULL, NULL, '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00', N'v1'),
    -- Two conflicting rows: same lane/vehicle/load_type, different rate_basis (per_ton vs per_cbm)
    -- -- not the flat+per_km-on-FTL exemption, so RateRowResolver must reject this outright.
    -- Stays on JHB_METRO:HARARE deliberately: VehicleSelectionServiceTest has no reefer fixture on
    -- this lane (only on JHB_METRO:BFN_METRO), so there's no winner/count collision risk here.
    ('40000000-0000-0000-0000-000000000035', N'JHB_METRO:HARARE', '30000000-0000-0000-0000-000000000018', N'reefer', N'per_ton', 200.0000, 'ZAR', NULL, NULL, NULL, '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00', N'v1'),
    ('40000000-0000-0000-0000-000000000036', N'JHB_METRO:HARARE', '30000000-0000-0000-0000-000000000018', N'reefer', N'per_cbm', 150.0000, 'ZAR', NULL, NULL, NULL, '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00', N'v1');
