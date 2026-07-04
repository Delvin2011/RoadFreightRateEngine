-- Pipeline Stage 6 (clearance & compliance charges) in-scope surcharge_rates rows. Several are
-- USD-denominated per the doc (COMESA Yellow Card, ZINARA, carbon tax are regional/cross-border
-- products typically quoted in USD); currency carried through unconverted, same as every prior
-- catalogue-driven stage.
INSERT INTO surcharge_rates
    (id, surcharge_code, surcharge_type, rate_value, currency, applies_to_vehicle_categories, applies_to_cargo_classes, applies_to_route_types, effective_from, effective_to, is_active, created_by, created_at)
VALUES
    -- effective_to is deliberately bounded (unlike the others, which are open-ended) so a test can
    -- exercise "this specific rate is missing" (asOfDate after 2025-12-31) in isolation, without
    -- every other cross-border fee's rate also going missing at the same date.
    ('50000000-0000-0000-0000-000000000018', N'BORDER_CLEARING_AGENT_FEE', N'flat', 1500.0000, 'ZAR', NULL, NULL, N'cross_border', '2025-01-01', '2025-12-31', 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00'),
    ('50000000-0000-0000-0000-000000000019', N'COMESA_LIABILITY_INSURANCE', N'flat', 45.0000, 'USD', NULL, NULL, N'cross_border', '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00'),
    ('50000000-0000-0000-0000-000000000020', N'SARS_CPF', N'flat', 250.0000, 'ZAR', NULL, NULL, N'cross_border', '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00'),
    ('50000000-0000-0000-0000-000000000021', N'ZINARA_ROAD_ACCESS_FEE', N'flat', 30.0000, 'USD', NULL, NULL, N'cross_border', '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00'),
    ('50000000-0000-0000-0000-000000000022', N'CARBON_TAX_LEVY', N'flat', 15.0000, 'USD', NULL, NULL, N'cross_border', '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00'),
    ('50000000-0000-0000-0000-000000000023', N'VET_HEALTH_CERTIFICATE', N'flat', 350.0000, 'ZAR', NULL, NULL, N'both', '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00'),
    ('50000000-0000-0000-0000-000000000024', N'ADG_TRANSPORT_PERMIT', N'flat', 600.0000, 'ZAR', NULL, NULL, N'both', '2025-01-01', NULL, 1, '00000000-0000-0000-0000-000000000001', '2025-01-01 00:00:00');

-- Test-only border post whose destination country is neither ZW nor MZ, so
-- ZinaraRoadAccessFeeRule/CarbonTaxLevyRule correctly don't trigger for it -- both existing seeded
-- border posts (BEIT_BRIDGE, TEST_BORDER_2) are ZA->ZW.
INSERT INTO border_posts (id, code, name, origin_country, destination_country) VALUES
    ('20000000-0000-0000-0000-000000000003', N'CLEARANCE_TEST_NON_ZIM_MOZ', N'Non-ZIM/MOZ Border Post (test fixture)', 'ZA', 'BW');
