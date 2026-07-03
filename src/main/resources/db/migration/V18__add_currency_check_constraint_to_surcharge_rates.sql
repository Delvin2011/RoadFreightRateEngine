-- Separate migration from V17 on purpose — see V17's comment for why the CHECK constraint can't
-- be appended to the same script that adds the column on SQL Server.
ALTER TABLE surcharge_rates ADD CONSTRAINT ck_surcharge_rates_currency CHECK (currency IN ('ZAR','USD','EUR'));
