-- Additional test-only fixtures for VehicleSelectionServiceTest.

-- REEFER_8T gets requires_permit=1 so VehicleSelectionResult.requiresPermit has a real true-case
-- to assert against (34T_SEMI, used elsewhere, stays at the default false).
UPDATE vehicle_categories SET requires_permit = 1 WHERE code = N'REEFER_8T';

INSERT INTO vehicle_categories (id, code, name, max_weight_kg, max_volume_cbm, description, zone_restriction, requires_permit) VALUES
    ('30000000-0000-0000-0000-000000000007', N'2T_RIGID', N'2-Ton Rigid', 2000.00, 10.00, N'Small rigid truck, used to pin the mixed-currency cost-comparison gap', NULL, 0),
    ('30000000-0000-0000-0000-000000000008', N'5T_RIGID_A', N'5-Ton Rigid A', 1800.00, 10.00, N'Tie-breaking fixture: identical capacity/cost to 5T_RIGID_B', NULL, 0),
    ('30000000-0000-0000-0000-000000000009', N'5T_RIGID_B', N'5-Ton Rigid B', 1800.00, 10.00, N'Tie-breaking fixture: identical capacity/cost to 5T_RIGID_A', NULL, 0),
    -- Capacity (3200kg/16cbm) is deliberately below the 3330kg chargeable weight used by the
    -- cost-efficient/dedicated-vehicle tests' fixture, so it doesn't become an unintended 4th
    -- candidate there — this vehicle has its own dedicated, smaller-weight fixture instead.
    ('30000000-0000-0000-0000-000000000010', N'7T_RIGID_LOWRATE', N'7-Ton Rigid (low per-km rate)', 3200.00, 16.00, N'Very low per_km rate but a high minimum_charge, to prove flooring changes the winner', NULL, 0);

INSERT INTO vehicle_category_load_types (id, vehicle_category_id, load_type) VALUES
    ('80000000-0000-0000-0000-000000000009', '30000000-0000-0000-0000-000000000007', N'ftl'),
    ('80000000-0000-0000-0000-000000000010', '30000000-0000-0000-0000-000000000008', N'ftl'),
    ('80000000-0000-0000-0000-000000000011', '30000000-0000-0000-0000-000000000009', N'ftl'),
    ('80000000-0000-0000-0000-000000000012', '30000000-0000-0000-0000-000000000010', N'ftl');

INSERT INTO road_freight_rates
    (id, lane_key, vehicle_category_id, load_type, rate_basis, rate_value, currency, minimum_charge, maximum_weight_kg, carrier_id, effective_from, effective_to, is_active, created_by, created_at, version_tag)
VALUES
    -- Cross-border happy path: JHB_METRO:HARARE, 8T_RIGID only (2T_RIGID's capacity excludes it
    -- at the weight/volume used for that test).
    ('40000000-0000-0000-0000-000000000007', N'JHB_METRO:HARARE', '30000000-0000-0000-0000-000000000001', N'ftl', N'per_km', 10.0000, 'ZAR', 1000.00, NULL, NULL, '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00', N'v1'),
    -- Mixed-currency fixture: numerically "cheaper" in USD (raw 6125) than 8T_RIGID's ZAR 12250,
    -- but not actually cheaper once currency is accounted for — pins the known comparison gap.
    ('40000000-0000-0000-0000-000000000008', N'JHB_METRO:HARARE', '30000000-0000-0000-0000-000000000007', N'ftl', N'per_km', 5.0000, 'USD', NULL, NULL, NULL, '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00', N'v1'),
    -- Tie-breaking fixture: identical flat rate for both 5T_RIGID_A and 5T_RIGID_B.
    ('40000000-0000-0000-0000-000000000009', N'JHB_METRO:HARARE', '30000000-0000-0000-0000-000000000008', N'ftl', N'flat', 3000.0000, 'ZAR', NULL, NULL, NULL, '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00', N'v1'),
    ('40000000-0000-0000-0000-000000000010', N'JHB_METRO:HARARE', '30000000-0000-0000-0000-000000000009', N'ftl', N'flat', 3000.0000, 'ZAR', NULL, NULL, NULL, '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00', N'v1'),
    -- Flooring fixture: raw cost (1.00 * 398km = 398.00) would be the cheapest of any vehicle on
    -- this lane, but minimum_charge=10000.00 floors it well above 8T_RIGID's 4776.00.
    ('40000000-0000-0000-0000-000000000011', N'JHB_METRO:BFN_METRO', '30000000-0000-0000-0000-000000000010', N'ftl', N'per_km', 1.0000, 'ZAR', 10000.00, NULL, NULL, '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00', N'v1');
