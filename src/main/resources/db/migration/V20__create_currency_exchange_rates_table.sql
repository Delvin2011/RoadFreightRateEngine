CREATE TABLE currency_exchange_rates (
    id              UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    from_currency   CHAR(3) NOT NULL,
    -- Modeled as a real column, not hardcoded to ZAR in the schema — Phase 1 only ever populates
    -- 'ZAR' here (all conversions target ZAR per the doc), but nothing stops a future non-ZAR
    -- target from being added without a schema change.
    to_currency     CHAR(3) NOT NULL,
    -- Exchange rates need more precision than money amounts (DECIMAL(12,4) elsewhere in this
    -- schema) — 6dp is standard practice for FX rates.
    rate            DECIMAL(18,6) NOT NULL,
    rate_date       DATE NOT NULL,
    source          NVARCHAR(100) NULL,
    -- No DB-level default, same audit-trail reasoning as road_freight_rates.created_at /
    -- surcharge_rates.created_at: always an explicit value from the application.
    created_at      DATETIME2 NOT NULL
);

CREATE INDEX ix_currency_exchange_rates_lookup ON currency_exchange_rates (from_currency, to_currency, rate_date);
