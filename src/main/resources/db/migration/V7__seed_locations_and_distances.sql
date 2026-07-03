-- Split from V6 into its own seed migration, mirroring the existing V3/V4 (schema) + V5 (seed)
-- convention rather than appending seed data directly onto the schema migration.

-- A destination-side zone is needed for the cross-border fixtures below; none of Stage 2's seed
-- zones are outside South Africa.
INSERT INTO zones (id, code, name, tier, country_code) VALUES
    ('10000000-0000-0000-0000-000000000004', N'HARARE', N'Harare', 1, 'ZW');

INSERT INTO locations (id, zone_id, name, address, location_type, created_at) VALUES
    ('60000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', N'Johannesburg Depot', N'1 Freight Ave, City Deep, Johannesburg', N'depot', '2025-01-01 00:00:00'),
    ('60000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000002', N'Bloemfontein Depot', N'10 Voortrekker Rd, Bloemfontein', N'depot', '2025-01-01 00:00:00'),
    ('60000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000004', N'Harare Client Site', N'5 Samora Machel Ave, Harare', N'client_site', '2025-01-01 00:00:00');

INSERT INTO lane_distances (id, origin_zone_id, destination_zone_id, border_post_id, distance_km, is_active, created_at) VALUES
    -- Domestic: JHB_METRO -> BFN_METRO, no border post.
    ('70000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002', NULL, 398.00, 1, '2025-01-01 00:00:00'),
    -- Cross-border: JHB_METRO -> HARARE via Beit Bridge.
    ('70000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000001', 1225.00, 1, '2025-01-01 00:00:00'),
    -- Synthetic fixture, not a real route: same JHB_METRO -> HARARE zone pair as the row above but
    -- with border_post_id NULL. Exists purely to prove the lookup treats border_post_id as part of
    -- the key rather than matching on zone pair alone (see LaneResolutionServiceTest).
    ('70000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000004', NULL, 1600.00, 1, '2025-01-01 00:00:00');
