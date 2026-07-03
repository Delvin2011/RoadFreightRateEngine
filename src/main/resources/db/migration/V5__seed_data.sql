-- Fixed UUID literals so Stage 2's verification test can reference known rows without a lookup-by-code round trip.

-- Seed/system admin, used as created_by for every row below.
-- '00000000-0000-0000-0000-000000000001'

INSERT INTO zones (id, code, name, tier, country_code) VALUES
    ('10000000-0000-0000-0000-000000000001', N'JHB_METRO', N'Johannesburg Metro', 1, 'ZA'),
    ('10000000-0000-0000-0000-000000000002', N'BFN_METRO', N'Bloemfontein Metro', 2, 'ZA'),
    ('10000000-0000-0000-0000-000000000003', N'LIMPOPO_RURAL', N'Limpopo Rural', 3, 'ZA');

INSERT INTO border_posts (id, code, name, origin_country, destination_country) VALUES
    ('20000000-0000-0000-0000-000000000001', N'BEIT_BRIDGE', N'Beit Bridge', 'ZA', 'ZW');

INSERT INTO vehicle_categories (id, code, name, max_weight_kg, max_volume_cbm, description) VALUES
    ('30000000-0000-0000-0000-000000000001', N'8T_RIGID', N'8-Ton Rigid', 8000.00, 40.00, N'8-ton rigid truck for local and regional distribution'),
    ('30000000-0000-0000-0000-000000000002', N'34T_SEMI', N'34-Ton Semi', 34000.00, 100.00, N'34-ton semi-trailer combination for long-haul FTL/LTL freight'),
    ('30000000-0000-0000-0000-000000000003', N'TANKER', N'Bulk Tanker', 30000.00, 30.00, N'Bulk liquid tanker for hazmat and fuel cargo');

-- created_at is supplied explicitly (UTC, matching effective_from) rather than left to a DB
-- default, per the audit-trail requirement on these tables.
INSERT INTO road_freight_rates
    (id, lane_key, vehicle_category_id, load_type, rate_basis, rate_value, currency, minimum_charge, maximum_weight_kg, carrier_id, effective_from, effective_to, is_active, created_by, created_at, version_tag)
VALUES
    ('40000000-0000-0000-0000-000000000001', N'JHB_METRO:BFN_METRO', '30000000-0000-0000-0000-000000000002', N'ftl', N'per_km', 18.5000, 'ZAR', 2500.00, NULL, NULL, '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00', N'v1'),
    ('40000000-0000-0000-0000-000000000002', N'JHB_METRO:BFN_METRO', '30000000-0000-0000-0000-000000000001', N'ltl', N'flat', 3500.0000, 'ZAR', NULL, NULL, NULL, '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00', N'v1'),
    ('40000000-0000-0000-0000-000000000003', N'JHB_METRO:BFN_METRO', '30000000-0000-0000-0000-000000000002', N'bulk_tipper', N'per_ton', 450.0000, 'ZAR', NULL, NULL, NULL, '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00', N'v1');

INSERT INTO surcharge_rates
    (id, surcharge_code, surcharge_type, rate_value, applies_to_vehicle_categories, applies_to_cargo_classes, applies_to_route_types, effective_from, effective_to, is_active, created_by, created_at)
VALUES
    ('50000000-0000-0000-0000-000000000001', N'FUEL_LEVY', N'pct_of_base', 0.2200, NULL, NULL, N'both', '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00'),
    ('50000000-0000-0000-0000-000000000002', N'TAIL_LIFT', N'flat', 450.0000, NULL, NULL, N'both', '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00');
