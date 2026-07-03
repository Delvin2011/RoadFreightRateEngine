-- Rate rows are immutable once activated: a rate change is always a new row (new effective_from),
-- with the superseded row's effective_to set to (change_date - 1 day). No UPDATE of rate_value
-- (or any other priced field) on an existing row is supported by the application layer.

CREATE TABLE road_freight_rates (
    id                    UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    -- Format ZONE_ORIGIN:ZONE_DEST (e.g. 'JHB_METRO:BFN_METRO'), per the Rate Table Design doc.
    -- Stored as a plain string for now; could become an FK pair to zones(id) later.
    lane_key              NVARCHAR(100) NOT NULL,
    -- Named vehicle_category_id (not vehicle_category) to follow standard FK column naming.
    vehicle_category_id   UNIQUEIDENTIFIER NOT NULL REFERENCES vehicle_categories(id),
    load_type             NVARCHAR(20) NOT NULL
                              CHECK (load_type IN ('ftl','ltl','flatbed','reefer','tanker','lowbed','side_tipper','bulk_tipper')),
    rate_basis            NVARCHAR(20) NOT NULL
                              CHECK (rate_basis IN ('per_km','per_ton','flat','per_pallet','per_cbm')),
    rate_value            DECIMAL(12,4) NOT NULL,
    currency              CHAR(3) NOT NULL CHECK (currency IN ('ZAR','USD','EUR')),
    minimum_charge        DECIMAL(12,2) NULL,
    maximum_weight_kg     DECIMAL(12,2) NULL,
    -- NULL = internal fleet; no FK yet, carriers table doesn't exist in this stage.
    carrier_id            UNIQUEIDENTIFIER NULL,
    effective_from        DATE NOT NULL,
    -- NULL = open-ended (still the current rate).
    effective_to          DATE NULL,
    is_active             BIT NOT NULL DEFAULT 1,
    created_by            UNIQUEIDENTIFIER NOT NULL,
    -- No DB-level default: this is the audit trail for who set a rate and when, so the value must
    -- come from the application layer (Instant.now()) or an explicit literal, never the DB server
    -- clock, which is not guaranteed to be UTC.
    created_at            DATETIME2 NOT NULL,
    version_tag           NVARCHAR(50) NULL
);

-- Date-bounded active-rate lookup is the hot path hit by Stage 4:
-- lane_key + vehicle_category_id + load_type + effective_from/effective_to (+ is_active).
CREATE INDEX idx_road_freight_rates_lookup
    ON road_freight_rates (lane_key, vehicle_category_id, load_type, effective_from, effective_to);

CREATE TABLE surcharge_rates (
    id                              UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    surcharge_code                  NVARCHAR(50) NOT NULL,
    surcharge_type                  NVARCHAR(20) NOT NULL
                                        CHECK (surcharge_type IN ('pct_of_base','flat','per_km','per_hour','per_unit')),
    -- Percentage as decimal (e.g. 0.2200 = 22%) when surcharge_type = 'pct_of_base'; a flat/unit
    -- amount for the other surcharge_type values.
    rate_value                      DECIMAL(10,4) NOT NULL,
    -- Comma-delimited vehicle_categories.code list; NULL = applies to all vehicle categories.
    applies_to_vehicle_categories   NVARCHAR(MAX) NULL,
    -- Comma-delimited Stage 1 CargoClass wire values; NULL = applies to all cargo classes.
    applies_to_cargo_classes        NVARCHAR(MAX) NULL,
    applies_to_route_types          NVARCHAR(20) NOT NULL
                                        CHECK (applies_to_route_types IN ('domestic','cross_border','both')),
    effective_from                  DATE NOT NULL,
    effective_to                    DATE NULL,
    is_active                       BIT NOT NULL DEFAULT 1,
    created_by                      UNIQUEIDENTIFIER NOT NULL,
    -- No DB-level default, for the same audit-trail reason as road_freight_rates.created_at above.
    created_at                      DATETIME2 NOT NULL
);

-- Date-bounded active-surcharge lookup is the hot path hit by Stage 5: surcharge_code + effective_from/effective_to.
CREATE INDEX idx_surcharge_rates_lookup
    ON surcharge_rates (surcharge_code, effective_from, effective_to);
