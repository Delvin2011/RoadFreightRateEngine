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

### Stage 3 — Input Validation (`...rateengine.validation`, `...rateengine.pipeline.common`)

Pipeline Stage 1 ("input validation & normalisation"): pure business-rule validation over the Stage 1 API DTOs. No persistence, no pricing — plain unit-testable Java, no Spring context needed to test it.

- **`InputValidationService`** — entry point; takes a `RateComputeRequest`, returns a `ValidationResult`. Runs every check and **accumulates all applicable errors in one pass** (no short-circuit on first failure), matching the API contract's multi-error example.
- **`ValidationResult`** — `errors` (hard failures, block pricing) and `flags` (informational, e.g. abnormal-dimension notices) as two independent lists; `isValid()` reflects `errors` only, so flags can be present on an otherwise-valid result.
- **Checks implemented**: `route_type = cross_border` requires `border_post_id` (`REQUIRED_FOR_CROSS_BORDER`); cargo class / load type compatibility (`INCOMPATIBLE_CARGO_CLASS`, via `CargoLoadTypeCompatibility`'s `Map<CargoClass, Set<LoadType>>` lookup); LTL requires `pallet_count > 0` (`PALLET_COUNT_REQUIRED_FOR_LTL`); reefer/perishable requires a valid `temperature_range_c` with `min <= max` (`TEMPERATURE_RANGE_REQUIRED`); hazmat requires `hazmat_un_number` and `hazmat_packing_group` (`REQUIRED_FOR_HAZMAT`, one error per missing field); `high_value_declared` requires `declared_value_zar > 0` (`REQUIRED_FOR_HIGH_VALUE`); oversized cargo requires `dimensions_lxwxh_m` (`REQUIRED_FOR_OVERSIZED`).
- **`LegalLimitsChecker`** — width > 2.4m / height > 4.3m / length > 22m are **soft flags** (`ABNORMAL_WIDTH`/`ABNORMAL_HEIGHT`/`ABNORMAL_LENGTH`; the load still gets priced, with permit surcharges added later in the pipeline). Chargeable weight > 56,000kg is the one **hard error** (`OVERWEIGHT`) — deliberately the interlink GCM ceiling rather than the standard 34,000kg limit, since Stage 3 runs before vehicle selection (Stage 5) and can't yet know whether an interlink combination is eligible for the route; this is a coarse sanity check only, and Stage 5 must apply its own tighter, vehicle-aware overweight check once the vehicle category is known.
- **`ChargeableWeightCalculator`** (`...pipeline.common`, not `...validation`, since Stage 5 and Stage 6 reuse it too) — `max(gross_weight_kg, volume_cbm × ROAD_VOLUMETRIC_FACTOR)`, factor = 333.
- `project_cargo` is **not** wired into any check — it's an independent Group D flag, not a cargo class, and does not bypass the compatibility matrix. (An earlier pass incorrectly treated it as a bypass condition, reading the source table's row structure too literally; that logic has been removed and is now covered by tests asserting it explicitly does *not* exempt an otherwise-incompatible pairing.)
- `INCOMPATIBLE_CARGO_CLASS` messages sort the permitted-options list before joining it into text — `Set.of()`'s iteration order is unspecified, so this keeps the message deterministic and safe to assert exactly in tests.
- Verified with `InputValidationServiceTest` (33 cases): every error code, the accumulate-don't-short-circuit behavior (a fixture with 3 simultaneous failures), the full compatibility matrix, exact boundary behavior at each legal limit (at-limit passes, just-over fails), flags/errors coexisting and staying independent, and exact `INCOMPATIBLE_CARGO_CLASS` message text.

### Stage 4 — Lane Resolution & Distance Computation (`...rateengine.laneresolution`)

Pipeline Stage 2: resolves an already-validated request's `origin_location_id`/`destination_location_id` to zones, builds the lane key, and resolves the distance. Top-level package, sibling to `...validation` — consistent with Stage 3 rather than nested under `...pipeline` (`...pipeline` stays reserved for genuinely cross-stage shared utilities like `ChargeableWeightCalculator`).

- **Prerequisite schema, missing from Stage 2**: `V6__create_location_and_distance_tables.sql` adds `locations` (what the request's location ids refer to) and `lane_distances` (the pre-computed distance matrix), with entities `Location`/`LaneDistance` and repositories under `...domain.entity`/`...domain.repository`, following Stage 2's conventions (UUID PKs, no setters, explicit `created_at`). `V7__seed_locations_and_distances.sql` seeds locations/distances plus a new `HARARE` zone — none of Stage 2's seeded zones were outside South Africa, and a cross-border fixture needs a destination-side zone.
- **`LaneResolutionService`** — takes the validated `RateComputeRequest`, returns a `LaneResolutionResult` (`laneKey`, `distanceKm`, `originZoneId`/`destinationZoneId`, `distanceOverrideApplied`). Explicit Javadoc precondition: assumes Stage 3 validation already ran (e.g. does not re-verify `border_post_id` is present for cross-border routes).
- **Distance resolution precedence**: an operator-supplied `route.distance_km` is used verbatim and bypasses the `lane_distances` lookup entirely, including for border-post-specific distances; otherwise resolves via `LaneDistanceRepository` keyed on origin zone, destination zone, **and `border_post_id`** — the same zone pair can have a domestic distance (null border post) and one or more distinct cross-border distances (different border posts) as separate rows.
- **Null-safe border-post lookup**: `LaneDistanceRepository.findActiveDistance` uses an explicit `LEFT JOIN ld.borderPost bp` rather than the implicit path `ld.borderPost.id` — implicit path navigation compiles to an INNER JOIN, which would silently exclude every domestic (null border post) row from ever matching. Verified against **both** H2 and real SQL Server, since this is new null-handling JPQL territory beyond the boolean-literal issue Stage 2 already found.
- **Three explicit exception types** (`UnknownLocationException`, `UnmappedZoneException`, `DistanceNotFoundException`), each carrying structured detail (location id + role, lane key + border post, etc.) rather than a bare message, so a later stage can translate them into the API error shape without re-deriving context. `UnmappedZoneException` (a `Location` with a dangling `zone_id`) can't occur through the real FK constraint, so it's covered by a separate Mockito unit test (`LaneResolutionServiceUnitTest`) mocking a `Zone` to simulate a Hibernate lazy proxy backed by a dangling reference.
- **`distance_override_reason` deliberately not added**: the Business Rules tab wants a reason recorded when an operator overrides distance, but `RouteRequest` (Stage 1) has no such field. Adding it properly means a Stage 1 DTO change plus an unspecified Stage 3 validation rule — real scope across two already-shipped stages, not a Stage 4 change. Explicitly deferred and tracked (see the `distance_override_reason_deferred` project memory), not left as an open-ended code comment.
- Verified with `LaneResolutionServiceTest` (12 cases, run against both H2 and real SQL Server) covering domestic/cross-border/override resolution, unknown origin/destination (including both invalid simultaneously, confirming origin is reported first), no-matching-distance, an inactive row with no active alternative, two different border posts for the same zone pair resolving to distinct distances, and a lane-key-ordering regression test (`JHB_METRO:BFN_METRO` vs. `BFN_METRO:JHB_METRO` are not treated as equivalent).

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

Flyway runs automatically on startup and creates/seeds the `items` table plus the rate engine's reference, rate, and lane resolution tables (`zones`, `border_posts`, `vehicle_categories`, `road_freight_rates`, `surcharge_rates`, `locations`, `lane_distances`).

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
