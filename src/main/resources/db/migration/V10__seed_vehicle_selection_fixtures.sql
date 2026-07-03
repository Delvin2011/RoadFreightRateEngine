-- New vehicle categories: a smaller rigid (4T_RIGID), a reefer-equipped vehicle (REEFER_8T), and a
-- metro-only vehicle (1T_BAKKIE) to exercise zone_restriction. TANKER already exists (Stage 2).
INSERT INTO vehicle_categories (id, code, name, max_weight_kg, max_volume_cbm, description, zone_restriction, requires_permit) VALUES
    ('30000000-0000-0000-0000-000000000004', N'4T_RIGID', N'4-Ton Rigid', 4000.00, 20.00, N'4-ton rigid truck for light local distribution', NULL, 0),
    ('30000000-0000-0000-0000-000000000005', N'REEFER_8T', N'8-Ton Reefer', 8000.00, 35.00, N'8-ton temperature-controlled reefer truck', NULL, 0),
    ('30000000-0000-0000-0000-000000000006', N'1T_BAKKIE', N'1-Ton Bakkie', 1000.00, 6.00, N'1-ton bakkie for metro-only light deliveries', N'METRO_ONLY', 0);

-- Load type eligibility, including for the pre-existing Stage 2 vehicle categories.
INSERT INTO vehicle_category_load_types (id, vehicle_category_id, load_type) VALUES
    ('80000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', N'ltl'),         -- 8T_RIGID
    ('80000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000001', N'ftl'),          -- 8T_RIGID
    ('80000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000002', N'ftl'),          -- 34T_SEMI
    ('80000000-0000-0000-0000-000000000004', '30000000-0000-0000-0000-000000000002', N'bulk_tipper'),  -- 34T_SEMI
    ('80000000-0000-0000-0000-000000000005', '30000000-0000-0000-0000-000000000003', N'tanker'),       -- TANKER
    ('80000000-0000-0000-0000-000000000006', '30000000-0000-0000-0000-000000000004', N'ftl'),          -- 4T_RIGID
    ('80000000-0000-0000-0000-000000000007', '30000000-0000-0000-0000-000000000005', N'reefer'),       -- REEFER_8T
    ('80000000-0000-0000-0000-000000000008', '30000000-0000-0000-0000-000000000006', N'ftl');          -- 1T_BAKKIE

-- New road_freight_rates rows for the new vehicles, on the existing JHB_METRO:BFN_METRO domestic
-- lane (distance 398.00km). Rates are chosen so that, for a mid-size shipment, 8T_RIGID is the
-- cheapest (cost-efficient pick) while 4T_RIGID is the smallest by capacity but NOT the cheapest
-- (dedicated-vehicle pick) — see VehicleSelectionServiceTest.
INSERT INTO road_freight_rates
    (id, lane_key, vehicle_category_id, load_type, rate_basis, rate_value, currency, minimum_charge, maximum_weight_kg, carrier_id, effective_from, effective_to, is_active, created_by, created_at, version_tag)
VALUES
    ('40000000-0000-0000-0000-000000000004', N'JHB_METRO:BFN_METRO', '30000000-0000-0000-0000-000000000001', N'ftl', N'per_km', 12.0000, 'ZAR', 1500.00, NULL, NULL, '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00', N'v1'),
    ('40000000-0000-0000-0000-000000000005', N'JHB_METRO:BFN_METRO', '30000000-0000-0000-0000-000000000004', N'ftl', N'per_km', 20.0000, 'ZAR', 1000.00, NULL, NULL, '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00', N'v1'),
    ('40000000-0000-0000-0000-000000000006', N'JHB_METRO:BFN_METRO', '30000000-0000-0000-0000-000000000005', N'reefer', N'per_km', 15.0000, 'ZAR', 2000.00, NULL, NULL, '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00', N'v1');
