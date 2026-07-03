# Road Freight Rate Engine

Spring Boot service for computing road freight rates. Package-by-feature: `items` (CRUD scaffold), `common`, and `rateengine` (the rate engine itself, in progress — see "Implementation Progress" below).

## Implementation Progress

### Stage 1 — Domain Model & DTOs (`...rateengine.api.dto`)

Request/response DTOs for `POST /api/v1/rate-engine/compute`. Plain Java records (matching this project's existing DTO convention), Jackson-annotated for a snake_case wire format. Structure only — no validation or business logic yet.

- **Request**: `RateComputeRequest` → nested `RouteRequest`, `CargoRequest`, `ServiceRequest`, plus small value records `Dimensions` and `TemperatureRange`.
- **Response**: `RateComputeResponse` → `LineItem`, `Totals`; error contract via `ValidationErrorResponse`/`ValidationError`.
- 8 enums (`RouteType`, `AddressType`, `CargoClass`, `LoadType`, `ContainerType`, `PackageType`, `PackingGroup`, `ServiceLevel`), each serializing to a lowercase snake_case wire value via an explicit `wireValue` field + `@JsonValue`/`@JsonCreator`.
- Verified with round-trip serialization tests (`RateComputeRequestSerializationTest`, `RateComputeResponseSerializationTest`) against real API contract fixtures (a hazmat tanker cross-border JHB→Harare quote).

### Stage 2 — Database Schema & JPA Entities (`...rateengine.domain`)

Flyway migrations plus JPA entities/repositories for the rate tables, targeting SQL Server (matching the existing project setup, not the PostgreSQL originally assumed by the stage spec).

- **Migrations**: `V3__create_reference_tables.sql` (`zones`, `border_posts`, `vehicle_categories`), `V4__create_rate_tables.sql` (`road_freight_rates`, `surcharge_rates` + the composite lookup indexes), `V5__seed_data.sql`.
- **Entities** (`...domain.entity`): `Zone`, `BorderPost`, `VehicleCategory`, `RoadFreightRate`, `SurchargeRate`, plus `RateBasis`/`SurchargeType`/`RouteApplicability` enums with JPA `AttributeConverter`s for lowercase snake_case DB storage.
- **Rate rows are immutable once activated**: no setters on priced fields (`rateValue`, `effectiveFrom`, etc.); only `expire(LocalDate)` may supersede an existing row (sets `effectiveTo` and clears `active`).
- `created_at` on both rate tables has **no DB-level default** — it's always an explicit value from the caller (application layer or an explicit literal in seed data), so the audit trail never silently depends on the DB server's clock/timezone.
- **Repositories** (`...domain.repository`): date-bounded active-rate/-surcharge lookups (`findActiveRate`, `findActiveSurcharges`, `findApplicableSurcharges`), following the pattern `effective_from <= :date AND (effective_to IS NULL OR effective_to >= :date) AND active`.
- Verified with `RateTablesIntegrationTest`, run against **both** H2 (default test suite) and a real SQL Server instance — this surfaced a genuine H2/SQL-Server divergence in how JPQL boolean predicates translate; see "H2 vs. real SQL Server: known divergence" under Test below.

## Prerequisites

- JDK 17
- Maven (or use the bundled `./mvnw` / `mvnw.cmd` wrapper — no local Maven install needed)
- A reachable Microsoft SQL Server instance (for running the app; not required for `mvnw test`, which uses an in-memory H2 database)

## Environment variables (required to run the app)

| Variable                     | Description                                  |
|-------------------------------|-----------------------------------------------|
| `SPRING_DATASOURCE_URL`       | JDBC URL, e.g. `jdbc:sqlserver://localhost:1433;databaseName=road_freight;encrypt=true;trustServerCertificate=true` |
| `SPRING_DATASOURCE_USERNAME`  | Database username                             |
| `SPRING_DATASOURCE_PASSWORD`  | Database password                             |

Optional:

| Variable                  | Description                          | Default |
|----------------------------|---------------------------------------|---------|
| `SERVER_PORT`              | HTTP port                             | `8080`  |
| `SPRING_PROFILES_ACTIVE`   | `dev` or `prod`                       | none — pass explicitly, see below |

## Run

```bash
export SPRING_DATASOURCE_URL="jdbc:sqlserver://localhost:1433;databaseName=road_freight;encrypt=true;trustServerCertificate=true"
export SPRING_DATASOURCE_USERNAME="sa"
export SPRING_DATASOURCE_PASSWORD="your-password"

./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Flyway runs automatically on startup and creates/seeds the `items` table plus the rate engine's reference and rate tables (`zones`, `border_posts`, `vehicle_categories`, `road_freight_rates`, `surcharge_rates`).

## Test

```bash
./mvnw test
```

Tests run against an in-memory H2 database (`src/test/resources/application.yml`, `MODE=MSSQLServer`) with Flyway enabled — the real `V*__*.sql` migrations run against H2 on every test run, so no external SQL Server instance or env vars are needed for the normal suite.

### H2 vs. real SQL Server: known divergence

H2's `MSSQLServer` compatibility mode approximates T-SQL *syntax*, not SQL Server's actual type-coercion rules. It has already passed JPQL that real SQL Server rejects:

- `WHERE r.active = true` — H2 accepts it; SQL Server errors with `Invalid column name 'true'` (it doesn't treat `true`/`false` as `BIT` literals).
- `WHERE r.active` (a bare boolean-column predicate) — H2 accepts it; SQL Server errors with `An expression of non-boolean type specified in a context where a condition is expected` (a `BIT` column can't stand alone as a boolean expression in T-SQL).

**Rule: any JPQL boolean predicate must be bound as a real parameter (`:active` supplied from Java), never inlined as a literal or a bare column reference.** See `RoadFreightRateRepository`/`SurchargeRateRepository` (`...rateengine.domain.repository`) for the pattern — a `default` method exposes the clean public signature while an internal `@Query` method binds `active` explicitly.

**Convention going forward**: any new query method with a boolean/`BIT`-column predicate should be run against a real SQL Server instance before it's considered done — H2 passing is not sufficient evidence. To do this ad hoc without changing any config file:

```bash
./mvnw test "-Dtest=YourNewTest" \
  "-Dspring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=RateEngine;encrypt=false;trustServerCertificate=true" \
  "-Dspring.datasource.username=sa" \
  "-Dspring.datasource.password=your-password" \
  "-Dspring.datasource.driver-class-name=com.microsoft.sqlserver.jdbc.SQLServerDriver"
```

`RateTablesIntegrationTest` (`...rateengine.domain.repository`) is the reference example of a test that's been run this way.

## Swagger / OpenAPI

Once running: http://localhost:8080/swagger-ui.html (raw spec at `/v3/api-docs`).
