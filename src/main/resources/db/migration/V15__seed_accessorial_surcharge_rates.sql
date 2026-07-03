-- Stage 7's flat accessorial rates. None of these existed before — Stage 2 only seeded FUEL_LEVY
-- and a single undifferentiated TAIL_LIFT row, not the collection/delivery split Stage 7 needs.
-- The existing TAIL_LIFT row is left untouched (presumably for the not-yet-built surcharges
-- pipeline stage, which may use a different code scheme) — these are new, distinct codes.
INSERT INTO surcharge_rates
    (id, surcharge_code, surcharge_type, rate_value, applies_to_vehicle_categories, applies_to_cargo_classes, applies_to_route_types, effective_from, effective_to, is_active, created_by, created_at)
VALUES
    ('50000000-0000-0000-0000-000000000003', N'AFTER_HOURS_COLLECTION', N'flat', 350.0000, NULL, NULL, N'both', '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00'),
    ('50000000-0000-0000-0000-000000000004', N'AFTER_HOURS_DELIVERY', N'flat', 350.0000, NULL, NULL, N'both', '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00'),
    ('50000000-0000-0000-0000-000000000005', N'TAIL_LIFT_COLLECTION', N'flat', 450.0000, NULL, NULL, N'both', '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00'),
    ('50000000-0000-0000-0000-000000000006', N'TAIL_LIFT_DELIVERY', N'flat', 450.0000, NULL, NULL, N'both', '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00'),
    ('50000000-0000-0000-0000-000000000007', N'DRIVER_ASSIST_LOADING', N'flat', 300.0000, NULL, NULL, N'both', '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00'),
    ('50000000-0000-0000-0000-000000000008', N'DRIVER_ASSIST_OFFLOADING', N'flat', 300.0000, NULL, NULL, N'both', '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00');
