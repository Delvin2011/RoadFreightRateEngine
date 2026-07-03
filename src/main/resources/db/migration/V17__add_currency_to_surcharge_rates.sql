-- Closes the structural gap in accessorial_currency_gap: surcharge_rates previously had no way
-- to express what currency a rate is denominated in at all. Mirrors road_freight_rates.currency
-- exactly (CHAR(3), same allowed-value set). The CHECK constraint is added in a separate
-- migration (V18), not appended here: SQL Server compiles a whole migration script as one batch
-- before executing any statement in it, so a CHECK constraint referencing this column within the
-- *same* script fails with "Invalid column name 'currency'" — the column genuinely has to exist as
-- of a prior, already-applied migration before a later one can constrain it (this is the actual
-- lesson from V9/V11: those were two separate migration files, not two statements in one file).
ALTER TABLE surcharge_rates ADD currency CHAR(3) NOT NULL DEFAULT 'ZAR';

-- Existing rows (FUEL_LEVY, TAIL_LIFT from V5; the 6 accessorial rates from V15;
-- SL_TEST_DATE_BOUNDARY from V16) all took the DEFAULT 'ZAR' above. Confirmed correct for every
-- one of them individually, not just accepted blindly: FUEL_LEVY and TAIL_LIFT are South African
-- domestic operational surcharges seeded alongside ZAR-denominated road_freight_rates rows on the
-- same JHB_METRO:BFN_METRO lane; the V15 accessorial rates (after-hours/tail-lift/driver-assist)
-- were authored in this codebase as South African freight accessorial amounts, always intended as
-- ZAR; SL_TEST_DATE_BOUNDARY is a test-only fixture with no currency significance of its own.
-- No individual backfill needed.
