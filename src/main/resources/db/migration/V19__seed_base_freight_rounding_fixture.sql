-- Step 0 of the Stage 8 prompt: audit found BaseFreightComputationService had the same class of
-- rounding bug Stage 7 had — ShipmentCostEstimator's PER_KM/PER_TON divide() carries 4dp
-- internally, but nothing rounded the *final* amount to 2dp before it became a BaseFreightResult
-- output. Every existing V13/V14 fixture happens to produce a numerically clean 2dp result (they
-- were all authored with round numbers), so the bug was real but never actually exercised by a
-- test. This fixture deliberately doesn't divide cleanly: 33.3350 * 3.00 * 1000 / 1000 = 100.0050
-- raw, which must round (HALF_UP) to 100.01, not silently stay at 4dp or truncate to 100.00.
INSERT INTO road_freight_rates
    (id, lane_key, vehicle_category_id, load_type, rate_basis, rate_value, currency, minimum_charge, maximum_weight_kg, carrier_id, effective_from, effective_to, is_active, created_by, created_at, version_tag)
VALUES
    ('40000000-0000-0000-0000-000000000031', N'BF_TEST_PERKM_DIRTY_PRECISION', '30000000-0000-0000-0000-000000000011', N'ftl', N'per_km', 33.3350, 'ZAR', NULL, NULL, NULL, '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00', N'v1');
