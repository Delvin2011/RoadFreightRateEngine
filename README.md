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
- **`distance_override_reason`**: originally deferred here (real scope across two already-shipped stages, not a Stage 4 change) — since fixed in a later consolidated pass (see "Consolidated deferred-gap closures" below). `RouteRequest` now carries the field, and `InputValidationService` enforces it.
- Verified with `LaneResolutionServiceTest` (12 cases, run against both H2 and real SQL Server) covering domestic/cross-border/override resolution, unknown origin/destination (including both invalid simultaneously, confirming origin is reported first), no-matching-distance, an inactive row with no active alternative, two different border posts for the same zone pair resolving to distinct distances, and a lane-key-ordering regression test (`JHB_METRO:BFN_METRO` vs. `BFN_METRO:JHB_METRO` are not treated as equivalent).

### Stage 5 — Vehicle Type Selection (`...rateengine.vehicleselection`)

Pipeline Stage 3: takes the validated request plus the Stage 4 lane result and selects a vehicle category — the eligible set by load type/capacity/zone restriction, then either the cheapest (default) or the smallest viable one (`dedicated_vehicle = true`), followed by a vehicle-aware overweight sanity check.

- **Prerequisite schema, missing from Stage 2**: `V9__extend_vehicle_categories_and_load_types.sql` adds the `vehicle_category_load_types` join table (a category can support several load types) and `zone_restriction`/`requires_permit` on `vehicle_categories`. `V11__add_zone_restriction_check_constraint.sql` later closes a gap — `zone_restriction` shipped in V9 without a `CHECK` constraint; V11 adds one, paired with a new `ZoneRestriction` enum + `ZoneRestrictionConverter` on the Java side (mirroring `RateBasis`/`RateBasisConverter`) instead of a raw string. `V10`/`V12` seed vehicles and rates.
- **Three-phase `VehicleSelectionService`**: `findEligibleVehicles` (Phase 1, throws `NoEligibleVehicleException` if nothing qualifies — never falls back to the largest vehicle) → `selectVehicle` (Phase 2, cheapest-or-smallest) → `checkVehicleCapacity` (Phase 3, a defensive final check against the *selected vehicle's actual* `max_weight_kg` — this is the vehicle-aware overweight check Stage 3 explicitly deferred; should be unreachable given Phase 1's filtering, and is exercised directly with an inconsistent input to prove the check itself works, not just that it's unreachable).
- **`ShipmentCostEstimator`** (`...pipeline.common`, not `...vehicleselection`, since Stage 6 needs identical math) — computes a `road_freight_rates` row's cost for a shipment per `rate_basis` (`per_km × distance`, `per_ton × chargeable_weight/1000`, `flat`, `per_pallet × pallet_count`, `per_cbm × volume`), applying `minimum_charge` as a floor.
- **Deterministic tie-breaking**: both the cost-efficient and dedicated-minimum-viable comparators have an explicit secondary sort key (vehicle code) — without it, a genuine cost or capacity tie would resolve based on incidental DB row order rather than being guaranteed reproducible.
- **`VehicleSelectionResult`** carries `requiresPermit` (the selected vehicle's own flag) alongside `selectedVehicleCategoryId`/`Code`, `selectionReason`, `eligibleVehicleCount` — an explicit decision to include it, so a later permit-surcharge pipeline stage doesn't need to re-query `vehicle_categories`.
- **Currency comparison is a known, pinned gap, not a fix**: cost-efficient selection compares raw `BigDecimal` cost across candidate rates with no FX conversion — a numerically smaller USD rate can "win" against a larger ZAR rate even when it's actually more expensive in real terms. Tracked as the `vehicle_selection_currency_comparison_deferred` project memory and pinned by a test that asserts the specific current (wrong) winner, so a future FX fix can't silently change this behavior unnoticed.
- Verified with `VehicleSelectionServiceTest` (13 cases, run against both H2 and real SQL Server): simple/cost-efficient/dedicated selection, zone-restriction exclusion, no-eligible-vehicle and no-rate-for-lane failures, the Phase 3 defensive check called directly, reefer load-type eligibility, deterministic tie-breaking, minimum_charge flooring changing the winner, the mixed-currency gap pinned, a single-eligible-vehicle dedicated path, and a cross-border-lane happy path (all prior happy-path cases were domestic).

### Stage 6 — Base Freight Rate Computation (`...rateengine.basefreight`)

Pipeline Stage 4: resolves which `road_freight_rates` row applies (more than one can legitimately be active for the same lane/vehicle/load type) and computes the BASE_FREIGHT line item from it.

- **Step 0 audit found a real bug**: `ShipmentCostEstimator`'s `PER_KM` case (built during Stage 5) was missing the `(chargeable_weight_kg / 1000)` factor entirely — it computed `rate × distance` only, not `rate × distance × chargeable_weight_kg / 1000` per the spec. Fixed. `PER_TON`/`FLAT`/`PER_PALLET` were already correct. Re-running the full Stage 5 suite post-fix changed **zero assertions** (every Stage 5 test compares `PER_KM` candidates under the same shipment, so the missing factor was a uniform scalar that didn't affect relative ranking) — but it did make 4 test comments/messages cite stale pre-fix dollar figures, corrected via `jshell` (not by hand) to avoid transcription errors in permanent documentation.
- **`RateRowResolver`** — fetches all active rows for the resolved lane/vehicle/load type. Exactly one row: use it. Zero rows: `RateNotFoundException`. More than one: the *only* auto-resolved case is exactly a `flat` + `per_km` pair on an **FTL** lane, where `flat` wins — literally scoped to that exact combination (a 3rd row, or the same flat/per_km pair on a non-FTL load type, still throws). Anything else: `AmbiguousRateConfigurationException`, carrying every conflicting row's id + `rate_basis`, never silently picked.
- **No explicit LTL code path**: dispatch is entirely by the resolved row's `rate_basis` (via `ShipmentCostEstimator`), not `cargo.load_type` — LTL's spec formula (`pallet_count × rate_per_pallet_per_lane`) is exactly the existing `PER_PALLET` case, so LTL lanes just need to be seeded with a `PER_PALLET` row. This is **not** a schema-enforced or documented exclusivity rule, though: `rate_basis` and `load_type` are independent `CHECK` constraints on `road_freight_rates`, so a `PER_PALLET` row could legitimately be seeded against a different load type in future (the formula is load-type-agnostic and would compute correctly) — documented explicitly in both `ShipmentCostEstimator` and `BaseFreightComputationService`'s Javadoc so a future reader doesn't mistake "true in current seed data" for an enforced invariant.
- **Minimum charge is a floor (`max`), not a cap (`min`), including for LTL** — the Business Rules tab's literal wording for the LTL case says `min(...)`, which contradicts both the field's own name and the general floor rule used for every other `rate_basis` in the same document. Treated as a documentation error, not followed literally; applied uniformly via `ShipmentCostEstimator.estimateWithDetail()`, a new method reporting whether the floor changed the result (the original `estimate()` now delegates to it, so Stage 5's usage is unaffected).
- **`BaseFreightResult`** carries `currency` straight from the matched row, unconverted (FX is a later stage's job), plus `rateBasisUsed`, `minimumChargeApplied`, and `lineItemComment` (exactly `"Minimum charge applied."` when the floor fired, `null` otherwise). `baseFreightAmount` is always rounded to exactly 2 decimal places (`HALF_UP`) — added retroactively during Stage 8's Step 0 audit (see Stage 8 below), which found `ShipmentCostEstimator`'s 4dp-precision `divide()` result was flowing straight through to this final output unrounded. Rounded at this service's boundary specifically, not inside `ShipmentCostEstimator` itself, since Stage 5 also uses that class for internal (non-final) cost comparison.
- Verified with `BaseFreightComputationServiceTest` (21 cases, run against both H2 and real SQL Server) across `V13`/`V14`/`V19` — fixtures use synthetic `lane_key` strings (that column has no FK) for full isolation from every other stage's data. Covers every rate basis, flat-vs-per-km precedence, the same precedence pairing rejected on a non-FTL load type (proving the FTL-scoping is enforced in code, not just documented), ambiguous/zero/inactive-only row handling, `effective_from`/`effective_to` boundary inclusivity, vehicle-category scoping between two rows sharing a lane/load type, an exact-equality minimum-charge boundary, a genuinely-null minimum charge, non-ZAR currency pass-through, `lineItemComment` being strictly `null` (not an empty string) when no flooring occurs, and (from the Step 0 fix) a dirty-precision fixture (`33.3350 × 3.00 × 1000 / 1000 = 100.0050` raw) proving the 2dp rounding actually fires.

### Stage 7 — Service Level & Accessorial Charges (`...rateengine.servicelevel`)

Pipeline Stage 7: applies the service level multiplier to `BASE_FREIGHT + Σ SURCHARGES + Σ CLEARANCES`, then appends accessorial charges (after-hours, tail lift, driver assist) as flat, unmultiplied line items. Built as an isolated consumer of `PreMultiplierTotals` — the surcharges (pipeline Stage 5) and clearances (pipeline Stage 6) stages don't exist in this codebase yet, so this stage has no dependency on them; they'll supply real sums instead of zero later, with no redesign needed here.

- **`ServiceLevelMultiplierResolver`** — named `BigDecimal` constants (Economy 1.00×, Standard 1.15×, Express 1.40×, Dedicated 1.65×) in an `EnumMap` lookup, not database-configurable per the spec's own design (unlike every other priced value in this codebase, which is always DB-sourced).
- **`AccessorialChargeCalculator`** sources flat amounts from `surcharge_rates` (`SurchargeRateRepository`, Stage 2) — never hardcoded, even though the surcharges pipeline stage itself isn't built. New seed rates in `V15`: `AFTER_HOURS_COLLECTION`/`DELIVERY`, `TAIL_LIFT_COLLECTION`/`DELIVERY`, `DRIVER_ASSIST_LOADING`/`OFFLOADING`. It's a thin pass-through for dates — `asOfDate` goes straight to the repository query with no calculator-level date logic of its own.
- **Tail-lift/driver-assist collection-vs-delivery split**: originally `ServiceRequest`'s `tail_lift_required`/`driver_assist_required` were single undifferentiated booleans that actively overpriced every single-ended shipment (both ends always applied) — since fixed in a later consolidated pass (see "Consolidated deferred-gap closures" below). `AccessorialChargeCalculator` now keys off 4 independent `ServiceRequest` fields directly.
- **"Waiting time" is not implemented**, and investigating its actual trigger condition ("declared at booking — exceeds 1hr free allowance") suggests it's not simply a missing `ServiceRequest` flag: the chargeable amount depends on real waiting duration, only known after the collection/delivery event — likely a post-event billing concern outside a pre-shipment quote engine's scope rather than an oversight here. Needs a business decision, not more engineering; tracked as the `accessorial_waiting_time_scope_question` project memory.
- **Accessorial currency, made explicit**: `surcharge_rates` originally had no `currency` column at all (`V17`/`V18` add one, mirroring `road_freight_rates.currency`, split across two migrations because SQL Server compiles a whole script as one batch and can't reference a column added earlier in the *same* script inside a `CHECK` constraint). `AccessorialChargeCalculator` now throws `AccessorialCurrencyMismatchException` if a resolved rate's currency doesn't match `PreMultiplierTotals.currency`, rather than silently summing incompatible currencies — Stage 8's `CurrencyConversionService` is the actual reconciliation mechanism (per-line-item, whole-pipeline), a different mechanism than this stage's own internal single-currency requirement, not a shortcut around it.
- **Rounding**: every amount this stage produces (`multipliedSubtotal`, the service-level uplift, every accessorial line item) is rounded to 2 decimal places (`HALF_UP`) — `multiply()` never rounds on its own, so a `1.15×` multiplier against a 2dp total otherwise produces an invalid 4dp "currency" value.
- **`ServiceLevelResult`** carries `currency` forward unconverted (same "carry through, don't convert" approach as Stage 6's `BaseFreightResult.currency()`), plus `multipliedSubtotal`, `serviceLevelLineItem` (the multiplier's dollar impact, `buy_zar = 0`, matching the API contract's `SERVICE_MULTIPLIER` example), `accessorialLineItems`, and `runningTotal`.
- Verified with `ServiceLevelComputationServiceTest` (18 cases) + `AccessorialChargeCalculatorUnitTest` (1 Mockito case, run against both H2 and real SQL Server) across `V15`–`V18`. Covers every service level, exact 2dp rounding on a value that doesn't divide cleanly, accessorials excluded from the multiplier, all four accessorial flags simultaneously with nothing dropped or double-counted, tail-lift/driver-assist applying independently per end (and together when both ends are flagged), `effective_from`/`effective_to` boundary inclusivity, unset flags never touching the repository (`verifyNoInteractions`), zero base freight, zero surcharges/clearances (today's actual reality), the currency mismatch throwing, and `runningTotal` checked against an independently-computed sum rather than a hardcoded literal.

### Stage 8 — Currency Conversion & VAT (`...rateengine.currencyconversion`)

Pipeline Stage 8, the final stage: converts every non-ZAR monetary line item to ZAR, sums to a subtotal, applies VAT, and produces the final `Totals`.

- **Step 0 reopened Stage 6**, as a clearly separated task with its own findings, not folded into this stage's own work: the same class of rounding bug Stage 7 had was also present in `BaseFreightComputationService` — confirmed real, fixed, documented above under Stage 6 rather than here.
- **`currency_exchange_rates`** (`V20`/`V21`) — `from_currency`/`to_currency`/`rate` (18,6 precision, more than money amounts need)/`rate_date`/`source`. `to_currency` is modeled as a real column (not hardcoded to ZAR in the schema) even though Phase 1 only ever populates `'ZAR'` there. Lookup policy: the most recent `rate_date` **on or before** the requested date, not an exact match — rates aren't published every day (weekends/holidays).
- **`CurrencyExchangeRateRepository`** implements that lookup as a plain `ORDER BY rate_date DESC` JPQL query with "take first" done in Java, deliberately *not* a `findFirstBy...OrderBy...` Spring Data derived query — that form compiles to a SQL Server `FETCH FIRST ... ROWS ONLY` clause this Hibernate/dialect combination can't emit correctly (H2 accepts it, real SQL Server rejects it with "Invalid usage of the option first in the FETCH statement"). See "H2 vs. real SQL Server: known divergence" below.
- **`CurrencyConversionService`** — pure consumer of `MonetaryLineItem`, deliberately decoupled from any upstream stage (same reasoning as Stage 7's `PreMultiplierTotals`; this stage doesn't know or care which pipeline stage produced a given line item). ZAR is hardcoded as the target currency in this Phase 1 *service logic* specifically — not in the schema, so a future non-ZAR target needs only a service change. Every converted amount rounded to 2dp `HALF_UP`.
- **`VatCalculationService`** — 15% is a fixed SA statutory rate, a single named constant (`VAT_RATE`), not database-configurable, same reasoning as Stage 7's fixed service-level multipliers (only exchange rates come from the DB in this stage). Zero-rating for SARS-classified exports is implemented (`computeVat(subtotal, zeroRated)`, both code paths tested) but currently unreachable in practice: `zeroRated` is hardcoded `false` at the only call site, since no `RateComputeRequest` field indicates export classification. Tracked as the `vat_zero_rating_deferred` project memory — deliberately left open (see "Consolidated deferred-gap closures" below for why this one, unlike the other two, wasn't picked up in the same pass).
- **`TotalsComputationService`** orchestrates both, producing `Totals` exactly matching Stage 1's DTO shape (`subtotalBuyZar`, `subtotalSellZar`, `vatZar`, `totalSellInclVatZar`); `marginPct` is always `null` — set by the quotation service, never this engine, per the doc.
- Verified with `CurrencyConversionServiceTest` (5 cases), `VatCalculationServiceTest` (3 cases), and `TotalsComputationServiceTest` (2 cases), run against both H2 and real SQL Server across `V19`–`V21`. Covers all-ZAR pass-through with an empty `exchangeRatesUsed`, mixed-currency conversion converting only the non-ZAR items, the most-recent-on-or-before policy (re-verified in isolation, standalone, after the dialect rewrite — not just as part of an aggregate pass), a before-any-rate-exists throw, conversion rounding on a value that doesn't divide cleanly, standard-rate and zero-rated VAT, VAT rounding, and `marginPct` always null.

### Consolidated deferred-gap closures

Three project-memory-tracked gaps had accumulated by the end of Stage 8: `distance_override_reason` (Stage 4), the tail-lift/driver-assist collection-vs-delivery split (Stage 7), and `vat_zero_rating` (Stage 8, above). Decided as a batch rather than continuing to defer indefinitely — two were straightforward field additions with no real ambiguity about what was needed; one is a genuine business decision, not an engineering one.

- **`distance_override_reason`**: added to `RouteRequest` (Stage 1), **and** made mandatory via a new hard Stage 3 validation error (`REQUIRED_FOR_DISTANCE_OVERRIDE`) whenever `route.distance_km` is set. The Business Rules tab uses the same "required" language as `REQUIRED_FOR_CROSS_BORDER`/`REQUIRED_FOR_HAZMAT` — both already hard errors — so an optional audit field would defeat the purpose of recording it; nothing would ever populate it.
- **Tail-lift/driver-assist split**: `ServiceRequest`'s `tail_lift_required`/`driver_assist_required` **removed entirely** (not kept alongside the replacements) and replaced with 4 independent fields: `tail_lift_collection`, `tail_lift_delivery`, `driver_assist_loading`, `driver_assist_offloading`. No live API consumers pre-release, so no backward-compat case to protect — keeping both old and new fields would only create an unspecified reconciliation problem for a scenario that can't occur once the old fields are gone. `AccessorialChargeCalculator` (Stage 7) updated to key off each flag directly.
- **`vat_zero_rating` deliberately left open**: different in kind from the other two — not a missing field with an obvious shape, but a missing *business classification* (what counts as a SARS-qualifying export, who determines that, at quote time or after). Genuinely a business decision to defer, not an engineering one.

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

Flyway runs automatically on startup and creates/seeds the `items` table plus the rate engine's reference, rate, lane resolution, vehicle selection, and currency conversion tables (`zones`, `border_posts`, `vehicle_categories`, `road_freight_rates`, `surcharge_rates`, `locations`, `lane_distances`, `vehicle_category_load_types`, `currency_exchange_rates`).

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

It also diverges on migration DDL, not just JPQL: SQL Server accepts `ALTER TABLE t ADD col1 type1, col2 type2` (multiple columns in one statement), but H2's `MSSQLServer` mode rejects it with a syntax error — split into separate `ALTER TABLE t ADD col1 type1;` / `ALTER TABLE t ADD col2 type2;` statements instead (see `V9__extend_vehicle_categories_and_load_types.sql`). `ALTER TABLE ... ADD CONSTRAINT ... CHECK (...)` on an existing column, by contrast, works fine on both engines — but a `CHECK` constraint referencing a column added *earlier in the same migration script* is a different story: SQL Server compiles a whole script as one batch before executing any statement in it, so the constraint's column reference fails to resolve ("Invalid column name") even though the `ADD COLUMN` statement precedes it textually. The column has to exist as of a prior, already-applied migration — split into two migration files instead (see `V17`/`V18`).

A third divergence, found in Stage 8: a Spring Data `findFirstBy...OrderBy...Desc` derived query (an implicit "top 1" lookup) compiles to a SQL Server `FETCH FIRST ? ROWS ONLY` clause via this project's Hibernate/dialect combination — H2 accepts it, real SQL Server rejects it ("Invalid usage of the option first in the FETCH statement"; SQL Server's `FETCH FIRST`/`FETCH NEXT` requires a paired `OFFSET` clause that Hibernate doesn't emit here). Fixed by replacing the derived query with a plain `@Query` using `ORDER BY ... DESC` and taking the first element of the returned list in Java, rather than relying on the database to apply the limit — see `CurrencyExchangeRateRepository.findMostRecentRateOnOrBefore`.

**Convention going forward**: any new query method with a boolean/`BIT`-column predicate, or any new migration using less-common DDL syntax, should be run against a real SQL Server instance before it's considered done — H2 passing is not sufficient evidence. To do this ad hoc without changing any config file:

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
