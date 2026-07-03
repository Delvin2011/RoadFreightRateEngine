-- Additional test-only fixtures for LaneResolutionServiceTest, added as a new migration rather
-- than editing V7 (already applied — editing an applied migration breaks Flyway checksums).

-- A second border post between the same zone pair as V7's Beit Bridge row, to prove per-border-post
-- distance resolution generalizes beyond just "a border post vs. no border post". Not a real border
-- post: ZA-ZW road freight realistically only has Beit Bridge as a significant crossing.
INSERT INTO border_posts (id, code, name, origin_country, destination_country) VALUES
    ('20000000-0000-0000-0000-000000000002', N'TEST_BORDER_2', N'Secondary Border Post (test fixture)', 'ZA', 'ZW');

INSERT INTO locations (id, zone_id, name, address, location_type, created_at) VALUES
    ('60000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000003', N'Limpopo Rural Depot', N'Main Rd, Musina', N'depot', '2025-01-01 00:00:00');

INSERT INTO lane_distances (id, origin_zone_id, destination_zone_id, border_post_id, distance_km, is_active, created_at) VALUES
    -- Same JHB_METRO -> HARARE zone pair as V7's Beit Bridge row (1225.00), but via the second
    -- border post above, with a distinct distance — proves the resolution isn't just "border vs.
    -- no border" but genuinely keyed per border post.
    ('70000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000002', 1400.00, 1, '2025-01-01 00:00:00'),
    -- Inactive, with no active alternative for this exact key (LIMPOPO_RURAL -> HARARE via Beit
    -- Bridge). Proves the repository excludes inactive rows rather than silently returning one.
    ('70000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000001', 1500.00, 0, '2025-01-01 00:00:00');
