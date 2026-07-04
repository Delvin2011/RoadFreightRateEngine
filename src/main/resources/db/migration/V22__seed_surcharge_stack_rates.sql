-- Stage 9's in-scope surcharges. FUEL_LEVY already exists (V5); TAIL_LIFT_* etc. from V15 are
-- Stage 7's accessorials, unrelated. These 8 are new.
INSERT INTO surcharge_rates
    (id, surcharge_code, surcharge_type, rate_value, currency, applies_to_vehicle_categories, applies_to_cargo_classes, applies_to_route_types, effective_from, effective_to, is_active, created_by, created_at)
VALUES
    ('50000000-0000-0000-0000-000000000010', N'HAZMAT_PG1_UPLIFT', N'pct_of_base', 0.1500, 'ZAR', NULL, NULL, N'both', '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00'),
    ('50000000-0000-0000-0000-000000000011', N'REEFER_RUNNING', N'per_km', 8.5000, 'ZAR', NULL, NULL, N'both', '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00'),
    -- Rate value here is the pct applied against REEFER_RUNNING's amount, not BASE_FREIGHT -- the
    -- documented exception (see FrozenGoodsUpliftRule).
    ('50000000-0000-0000-0000-000000000012', N'FROZEN_GOODS_UPLIFT', N'pct_of_base', 0.2000, 'ZAR', NULL, NULL, N'both', '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00'),
    -- Rate value here is the pct applied against cargo.declared_value_zar, not BASE_FREIGHT.
    ('50000000-0000-0000-0000-000000000013', N'HIGH_VALUE_INSURANCE_LEVY', N'pct_of_base', 0.0100, 'ZAR', NULL, NULL, N'both', '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00'),
    ('50000000-0000-0000-0000-000000000014', N'LIVE_ANIMAL_WELFARE', N'flat', 800.0000, 'ZAR', NULL, NULL, N'both', '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00'),
    ('50000000-0000-0000-0000-000000000015', N'LIVESTOCK_VEHICLE_CERT', N'flat', 1200.0000, 'ZAR', NULL, NULL, N'both', '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00'),
    ('50000000-0000-0000-0000-000000000016', N'FRAGILE_HANDLING', N'pct_of_base', 0.0800, 'ZAR', NULL, NULL, N'both', '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00'),
    -- Rate value here is the INCREMENTAL fraction (0.50 = the extra half needed to reach the
    -- documented "1.5x pallet rate" total) -- see NonStackableSpaceFactorRule.
    ('50000000-0000-0000-0000-000000000017', N'NON_STACKABLE_SPACE_FACTOR', N'pct_of_base', 0.5000, 'ZAR', NULL, NULL, N'both', '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00');
