-- Extends vehicle_categories with fields Stage 2 didn't include, and adds the
-- vehicle_category_load_types join table (a vehicle category can support multiple load types,
-- e.g. a 34-ton semi doing both FTL and bulk_tipper work).

-- H2's MSSQLServer mode doesn't support the multi-column "ADD col1 type1, col2 type2" form SQL
-- Server itself accepts — split into separate ALTER TABLE statements for portability.
ALTER TABLE vehicle_categories ADD zone_restriction NVARCHAR(30) NULL;
ALTER TABLE vehicle_categories ADD requires_permit BIT NOT NULL DEFAULT 0;

CREATE TABLE vehicle_category_load_types (
    id                    UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    vehicle_category_id   UNIQUEIDENTIFIER NOT NULL REFERENCES vehicle_categories(id),
    load_type             NVARCHAR(20) NOT NULL
                              CHECK (load_type IN ('ftl','ltl','flatbed','reefer','tanker','lowbed','side_tipper','bulk_tipper')),
    CONSTRAINT uq_vehicle_category_load_types UNIQUE (vehicle_category_id, load_type)
);

-- The lookup Stage 5 hits for every request: "which vehicle categories support this load type".
CREATE INDEX idx_vehicle_category_load_types_lookup ON vehicle_category_load_types (load_type);
