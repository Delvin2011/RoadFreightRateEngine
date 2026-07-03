-- Controlled vocabularies maintained by rate admins (not Java enums), referenced by the rate tables in V4.

CREATE TABLE zones (
    id            UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    code          NVARCHAR(50) NOT NULL UNIQUE,
    name          NVARCHAR(255) NOT NULL,
    tier          INT NOT NULL CHECK (tier BETWEEN 1 AND 4),
    country_code  CHAR(2) NOT NULL
);

CREATE TABLE border_posts (
    id                   UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    code                 NVARCHAR(50) NOT NULL UNIQUE,
    name                 NVARCHAR(255) NOT NULL,
    origin_country       CHAR(2) NOT NULL,
    destination_country  CHAR(2) NOT NULL
);

CREATE TABLE vehicle_categories (
    id              UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
    code            NVARCHAR(50) NOT NULL UNIQUE,
    name            NVARCHAR(255) NOT NULL,
    max_weight_kg   DECIMAL(10,2) NOT NULL,
    max_volume_cbm  DECIMAL(10,2) NOT NULL,
    description     NVARCHAR(500) NOT NULL
);
