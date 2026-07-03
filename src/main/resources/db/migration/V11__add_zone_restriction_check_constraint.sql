-- V9 added zone_restriction without a CHECK constraint. Closing that gap now that the Java side
-- has been tightened to an enum (ZoneRestriction) — the DB should enforce the same restriction.
-- Only METRO_ONLY exists today; extend this list (and ZoneRestriction) together if a new value
-- is ever needed.
ALTER TABLE vehicle_categories ADD CONSTRAINT ck_vehicle_categories_zone_restriction
    CHECK (zone_restriction IS NULL OR zone_restriction IN ('METRO_ONLY'));
