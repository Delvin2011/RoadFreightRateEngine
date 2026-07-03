-- 3 USD->ZAR rows and 2 EUR->ZAR rows across different rate_date values, so the "most recent
-- on or before" lookup policy has something meaningful to select between in tests. 18.52 on
-- 2025-06-01 matches the USD_ZAR figure already used in Stage 1's API contract fixture example.
INSERT INTO currency_exchange_rates (id, from_currency, to_currency, rate, rate_date, source, created_at)
VALUES
    ('60000000-0000-0000-0000-000000000001', N'USD', N'ZAR', 17.900000, '2025-01-01', N'SARB', '2025-01-01 00:00:00'),
    ('60000000-0000-0000-0000-000000000002', N'USD', N'ZAR', 18.520000, '2025-06-01', N'SARB', '2025-06-01 00:00:00'),
    ('60000000-0000-0000-0000-000000000003', N'USD', N'ZAR', 18.750000, '2025-07-01', N'SARB', '2025-07-01 00:00:00'),
    ('60000000-0000-0000-0000-000000000004', N'EUR', N'ZAR', 19.400000, '2025-01-01', N'SARB', '2025-01-01 00:00:00'),
    ('60000000-0000-0000-0000-000000000005', N'EUR', N'ZAR', 19.800000, '2025-06-01', N'SARB', '2025-06-01 00:00:00');
