-- Prerequisite tables missing from Stage 2: locations (what origin_location_id/destination_location_id
-- in the API request refer to) and lane_distances (the pre-computed distance matrix).

CREATE TABLE locations (
    id             UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    zone_id        UNIQUEIDENTIFIER NOT NULL REFERENCES zones(id),
    name           NVARCHAR(255) NOT NULL,
    address        NVARCHAR(500) NULL,
    -- Informational only (e.g. depot, port, inland_depot, client_site) — not used for logic in
    -- this stage. Plain string rather than a Java enum: values aren't final, and this domain
    -- table must stay independent of the Stage 1 API DTOs' AddressType enum.
    location_type  NVARCHAR(30) NULL,
    -- No DB-level default, per the audit-trail convention established in Stage 2: created_at is
    -- always an explicit value from the caller.
    created_at     DATETIME2 NOT NULL
);

CREATE TABLE lane_distances (
    id                    UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    origin_zone_id        UNIQUEIDENTIFIER NOT NULL REFERENCES zones(id),
    destination_zone_id   UNIQUEIDENTIFIER NOT NULL REFERENCES zones(id),
    -- NULL = domestic lane. Populated for cross-border lanes: the same origin/destination zone
    -- pair can have different distances depending on which border post is used, so border_post_id
    -- is part of the lookup key, not just an informational column.
    border_post_id        UNIQUEIDENTIFIER NULL REFERENCES border_posts(id),
    distance_km           DECIMAL(10,2) NOT NULL,
    is_active             BIT NOT NULL DEFAULT 1,
    created_at            DATETIME2 NOT NULL
);

-- The lookup Stage 4 hits for every request.
CREATE INDEX idx_lane_distances_lookup
    ON lane_distances (origin_zone_id, destination_zone_id, border_post_id);
