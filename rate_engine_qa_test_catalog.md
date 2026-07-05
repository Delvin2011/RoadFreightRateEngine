# Road Freight Rate Engine — API Test Scenario Catalog

40 scenarios for `POST /api/v1/rate-engine/compute`. Each one includes a **Purpose** note explaining what capability of the system it exercises and why that capability exists — the goal is for your team to come away understanding how the engine actually works, not just whether a given request returns the expected status code.

## How the engine works, in brief

Every request runs through a fixed sequence: validate the input → resolve the route to a lane and distance → select a vehicle → compute base freight from a rate table → add applicable surcharges → add cross-border/compliance charges (if any) → apply a service-level multiplier and any accessorial charges → convert everything to ZAR and apply VAT. Every priced value comes from a database rate table, never a hardcoded number — so the exact rand amounts below reflect what's currently seeded, and could change if rates are updated. What should **not** change is the *logic*: which line items appear, in what order, and under what conditions.

## Before you start

- **Base URL** below assumes local: `http://localhost:8080`. Adjust for your environment.
- **Fixture dependency**: scenarios 5, 6, and 23 rely on test-specific seed data (`V24` migration — an EUR-rated vehicle, a dedicated-vs-cost-efficient vehicle pair, and a deliberately ambiguous rate configuration). Confirm your target database has been migrated through `V24` before running these — otherwise they'll fail for the wrong reason (missing data), not a real defect.
- All UUIDs below are real, seeded values from the project's Flyway migrations — not placeholders.
- Every JSON body uses snake_case field names, matching the live API contract.

| Constant | UUID | What it is |
|---|---|---|
| `JHB_LOCATION_ID` | `60000000-0000-0000-0000-000000000001` | Johannesburg metro location |
| `BFN_LOCATION_ID` | `60000000-0000-0000-0000-000000000002` | Bloemfontein location |
| `HARARE_LOCATION_ID` | `60000000-0000-0000-0000-000000000003` | Harare location (cross-border) |
| `LIMPOPO_RURAL_LOCATION_ID` | `60000000-0000-0000-0000-000000000004` | Limpopo rural location |
| `BEIT_BRIDGE_ID` | `20000000-0000-0000-0000-000000000001` | Beit Bridge border post |
| `TEST_BORDER_2_ID` | `20000000-0000-0000-0000-000000000002` | Second test-only border post |

Collection date used throughout: `2025-07-15`.

---

## A. Happy paths (expect HTTP 200)

### 1. Cross-border hazmat — full pipeline, every charge category present

**Purpose**: this is the single most comprehensive request the engine can handle — it's designed to touch every stage of the pipeline in one shot: base freight, a percentage surcharge, cross-border clearance fees, a compliance permit, currency conversion, and the service multiplier. Use this one to build your mental model of "what a fully-loaded response looks like" before testing narrower cases.

```bash
curl -X POST http://localhost:8080/api/v1/rate-engine/compute \
  -H "Content-Type: application/json" \
  -d '{
    "quote_context_id": "11111111-0000-0000-0000-000000000001",
    "rate_date": "2025-07-15",
    "route": {
      "origin_location_id": "60000000-0000-0000-0000-000000000001",
      "destination_location_id": "60000000-0000-0000-0000-000000000003",
      "route_type": "cross_border",
      "border_post_id": "20000000-0000-0000-0000-000000000001",
      "collection_address_type": "depot",
      "delivery_address_type": "door_to_door"
    },
    "cargo": {
      "cargo_class": "hazmat",
      "commodity_code": "2710.12.90",
      "gross_weight_kg": 5000,
      "volume_cbm": 10,
      "load_type": "ftl",
      "package_type": "bulk",
      "stackable": true,
      "hazmat_un_number": "UN1203",
      "hazmat_packing_group": "I",
      "imdg_class": "3",
      "high_value_declared": false,
      "security_escort_required": false,
      "abnormal_load": false,
      "live_animals": false,
      "project_cargo": false
    },
    "service": {
      "service_level": "standard",
      "collection_date": "2025-07-15",
      "after_hours_collection": false,
      "after_hours_delivery": false,
      "tail_lift_collection": false,
      "tail_lift_delivery": false,
      "driver_assist_loading": false,
      "driver_assist_offloading": false,
      "dedicated_vehicle": false,
      "security_escort_required": false
    }
  }'
```

**Expect**: HTTP 200. `vehicle_selected: "8T_RIGID"`, `distance_km: 1225.00`, line items include `BASE_FREIGHT`, `FUEL_LEVY`, `HAZMAT_PG1_UPLIFT`, `BORDER_CLEARING_AGENT_FEE`, `COMESA_LIABILITY_INSURANCE`, `SARS_CPF`, `ZINARA_ROAD_ACCESS_FEE`, `CARBON_TAX_LEVY`, `ADG_TRANSPORT_PERMIT`, `SERVICE_MULTIPLIER`. `exchange_rates_used` contains `USD_ZAR`. `totals.total_sell_incl_vat_zar: 116038.31`.

Verified end-to-end against the running app (2026-07-05): exact match on every field above, including the total to the cent.

### 2. Domestic general cargo — the simplest possible valid quote

**Purpose**: this is the baseline. No hazmat, no cross-border, no special flags — just a straightforward domestic FTL shipment. Every other scenario in this catalog is really "scenario 2, plus one thing changed." Understanding this response fully makes every other scenario easier to reason about, since you'll be able to spot exactly what's different.

```bash
curl -X POST http://localhost:8080/api/v1/rate-engine/compute \
  -H "Content-Type: application/json" \
  -d '{
    "quote_context_id": "11111111-0000-0000-0000-000000000002",
    "rate_date": "2025-07-15",
    "route": {
      "origin_location_id": "60000000-0000-0000-0000-000000000001",
      "destination_location_id": "60000000-0000-0000-0000-000000000002",
      "route_type": "domestic",
      "collection_address_type": "depot",
      "delivery_address_type": "door_to_door"
    },
    "cargo": {
      "cargo_class": "general",
      "commodity_code": "8481.80",
      "gross_weight_kg": 5000,
      "volume_cbm": 10,
      "load_type": "ftl",
      "package_type": "palletised",
      "stackable": true,
      "high_value_declared": false,
      "security_escort_required": false,
      "abnormal_load": false,
      "live_animals": false,
      "project_cargo": false
    },
    "service": {
      "service_level": "standard",
      "collection_date": "2025-07-15",
      "after_hours_collection": false,
      "after_hours_delivery": false,
      "tail_lift_collection": false,
      "tail_lift_delivery": false,
      "driver_assist_loading": false,
      "driver_assist_offloading": false,
      "dedicated_vehicle": false,
      "security_escort_required": false
    }
  }'
```

**Expect**: HTTP 200. `vehicle_selected: "8T_RIGID"`, `distance_km: 398.00`, line items exactly `BASE_FREIGHT`, `FUEL_LEVY`, `SERVICE_MULTIPLIER`. `flags: []`, `requires_manual_review: false`.

Verified end-to-end against the running app (2026-07-05): exact match. (`8T_RIGID` at 12.00 ZAR/km genuinely beats `34T_SEMI` at 18.50 ZAR/km on this lane at this weight — not a coincidental pick.)

### 3. Domestic fragile cargo — a surcharge without a clearance charge

**Purpose**: demonstrates that surcharges and clearance charges are independent systems. Fragile cargo triggers a percentage surcharge regardless of route type, but produces zero clearance charges since it's domestic — proving these two charge categories don't leak into each other.

Same route as #2, but `cargo_class: "fragile"`. **Expect**: HTTP 200, line items `BASE_FREIGHT`, `FUEL_LEVY`, `FRAGILE_HANDLING`, `SERVICE_MULTIPLIER` — no clearance charges.

### 4. Hazmat + live animals + high value, combined — a known gap, not a bug

**Purpose**: this is the most important scenario in the whole catalog for your team to understand *before* they start filing tickets. It combines three flags that each individually feel like they should trigger manual review, but the engine's manual-review logic hasn't been built yet — `requires_manual_review` is currently hardcoded `false` for every request, no matter what's in it. Running this scenario and seeing `false` is **expected, current, correct behavior** — not something to report.

Same route as #2, `cargo_class: "hazmat"`, `hazmat_un_number: "UN1203"`, `hazmat_packing_group: "I"`, `imdg_class: "3"`, `live_animals: true`, `high_value_declared: true`, `declared_value_zar: 500000.00`.

**Expect**: HTTP 200, `requires_manual_review: false`.

### 5. Dedicated vehicle — capacity wins over cost (requires V24 fixtures)

**Purpose**: the engine has two distinct vehicle-selection strategies. By default, it picks the cheapest vehicle that can carry the cargo. But when a customer specifically books a dedicated (exclusive-use) vehicle, cost stops mattering entirely — the engine picks the smallest vehicle that physically fits the cargo, even if a cheaper, larger option exists. This scenario is built specifically so those two strategies would pick *different* vehicles, so you can see the distinction directly rather than take it on faith.

```bash
curl -X POST http://localhost:8080/api/v1/rate-engine/compute \
  -H "Content-Type: application/json" \
  -d '{
    "quote_context_id": "11111111-0000-0000-0000-000000000005",
    "rate_date": "2025-07-15",
    "route": {
      "origin_location_id": "60000000-0000-0000-0000-000000000004",
      "destination_location_id": "60000000-0000-0000-0000-000000000003",
      "route_type": "cross_border",
      "border_post_id": "20000000-0000-0000-0000-000000000002",
      "collection_address_type": "depot",
      "delivery_address_type": "door_to_door"
    },
    "cargo": {
      "cargo_class": "general",
      "commodity_code": "8481.80",
      "gross_weight_kg": 1100,
      "volume_cbm": 2,
      "load_type": "ftl",
      "package_type": "palletised",
      "stackable": true,
      "high_value_declared": false,
      "security_escort_required": false,
      "abnormal_load": false,
      "live_animals": false,
      "project_cargo": false
    },
    "service": {
      "service_level": "standard",
      "collection_date": "2025-07-15",
      "after_hours_collection": false,
      "after_hours_delivery": false,
      "tail_lift_collection": false,
      "tail_lift_delivery": false,
      "driver_assist_loading": false,
      "driver_assist_offloading": false,
      "dedicated_vehicle": true,
      "security_escort_required": false
    }
  }'
```

**Expect**: HTTP 200, `vehicle_selected: "SMALL_EXPENSIVE_TEST"` — not the cheaper, larger-capacity alternative.

Verified end-to-end against the running app (2026-07-05): exact match, `BASE_FREIGHT: 9000.00` (the flat rate seeded specifically for this vehicle).

### 6. Three currencies at once (requires V24 fixtures)

**Purpose**: rate tables can be denominated in ZAR, USD, or EUR, and a single shipment can genuinely involve all three at once (a foreign-carrier rate, a USD-denominated border fee, a EUR-denominated permit, for instance). This scenario proves the engine correctly converts every component to ZAR independently before summing them — rather than, say, accidentally treating a "45.00" USD line item as if it were already 45.00 ZAR. This is one of the more failure-prone parts of the system conceptually, so it's worth your team understanding it deeply rather than just confirming the total looks roughly right.

Same route as #5, but `load_type: "flatbed"`, `gross_weight_kg: 8500`. **Expect**: HTTP 200, `vehicle_selected: "9T_EUR_TEST"`, `exchange_rates_used` contains both `EUR_ZAR` and `USD_ZAR`. Note: `FUEL_LEVY` is shown in EUR-converted terms (it inherits the base freight's currency), not the ZAR value its own underlying rate row would suggest — worth specifically checking this line item's math.

Verified end-to-end against the running app (2026-07-05): exact match. `BASE_FREIGHT: 1884960.00` (95200.00 EUR base freight × the seeded 19.8 EUR_ZAR rate), `FUEL_LEVY: 414691.20` (20944.00 EUR, 22% of the EUR base freight, × 19.8 — confirms the EUR-inheritance behavior is real, not just documented).

### 7. Distance override with an abnormal-width flag

**Purpose**: operators can manually override the system's computed distance (e.g. for a known shortcut route), but the engine requires a reason to be recorded whenever they do, for audit purposes. Separately, cargo that exceeds legal road dimensions doesn't get rejected — it gets priced normally but flagged, since a permit can usually be obtained. This scenario shows both features working together in one response.

Route JHB to BFN (domestic), with `distance_km: 500.00` and `distance_override_reason: "Customer-confirmed shorter route via toll road"` added to the `route` object, plus cargo `dimensions_lxwxh_m: [10, 2.6, 2.5]` (width 2.6m exceeds the 2.4m legal limit).

**Expect**: HTTP 200, `distance_km: 500.00` (not the lane's normal 398.00), `flags` contains `DISTANCE_OVERRIDE` and `ABNORMAL_WIDTH`. `requires_manual_review` stays `false` — these are soft flags, not hard blocks.

### 8. Distance override with abnormal height and length together

**Purpose**: confirms multiple independent flags can appear on the same response without one crowding out another — a different pair of dimensions than #7, to prove this isn't a coincidence of one specific flag combination.

Same as #7, but `dimensions_lxwxh_m: [22.01, 2, 4.31]`. **Expect**: `flags` contains `DISTANCE_OVERRIDE`, `ABNORMAL_HEIGHT`, `ABNORMAL_LENGTH` — not `ABNORMAL_WIDTH`.

---

## B. Validation errors (expect HTTP 422)

**Purpose for this whole section**: the engine validates every request thoroughly before doing any pricing work at all, and it reports *every* problem it finds in one response rather than making the caller fix one error, resubmit, and discover the next one. Each scenario below tests one specific business rule; #18 specifically tests the "report everything at once" behavior.

Each of these starts from scenario #2's domestic general-cargo body and changes one thing, unless noted.

### 9. Cross-border without a border post

Change `route_type` to `"cross_border"` and `destination_location_id` to Harare's UUID, omit `border_post_id` entirely.
**Expect**: 422, one error, `code: "REQUIRED_FOR_CROSS_BORDER"`.

### 10. Distance override without a reason

Add `distance_km: 500.00` to the route, omit `distance_override_reason`.
**Expect**: 422, `code: "REQUIRED_FOR_DISTANCE_OVERRIDE"`.

### 11. Hazmat missing UN number

Set `cargo_class: "hazmat"`, `hazmat_packing_group: "I"`, omit `hazmat_un_number`.
**Expect**: 422, `code: "REQUIRED_FOR_HAZMAT"`, referencing `hazmat_un_number`.

### 12. Hazmat missing packing group

Inverse of #11 — provide `hazmat_un_number`, omit `hazmat_packing_group`.
**Expect**: 422, `code: "REQUIRED_FOR_HAZMAT"`, referencing `hazmat_packing_group`. Confirms both hazmat fields are checked independently, not as a single combined check.

### 13. LTL without pallet count

Set `load_type: "ltl"`, omit `pallet_count`.
**Expect**: 422, `code: "PALLET_COUNT_REQUIRED_FOR_LTL"`.

### 14. Reefer without a temperature range

Set `load_type: "reefer"`, omit `temperature_range_c`.
**Expect**: 422, `code: "TEMPERATURE_RANGE_REQUIRED"`.

### 15. High value declared without a value

Set `high_value_declared: true`, omit `declared_value_zar`.
**Expect**: 422, `code: "REQUIRED_FOR_HIGH_VALUE"`.

### 16. Oversized cargo without dimensions

Set `cargo_class: "oversized"`, omit `dimensions_lxwxh_m`.
**Expect**: 422, `code: "REQUIRED_FOR_OVERSIZED"`.

### 17. Incompatible cargo class and load type

Set `cargo_class: "hazmat"` (with valid hazmat fields) and `load_type: "ltl"` (with `pallet_count` provided).
**Expect**: 422, `code: "INCOMPATIBLE_CARGO_CLASS"`. This rule cannot be bypassed by any other flag, including `project_cargo: true` — worth trying that combination too, if your team wants to specifically confirm there's no backdoor around it.

### 18. Multiple simultaneous validation errors

Combine #9 and #11 in one request.
**Expect**: 422, two errors in the `errors` array — the core proof that the engine reports everything wrong at once.

### 19. Overweight cargo

Set `gross_weight_kg: 60000`.
**Expect**: 422, `code: "OVERWEIGHT"`. Note: this ceiling (56,000kg) is deliberately generous — it's a coarse sanity check that assumes the largest possible legal vehicle combination, not the specific vehicle that will end up being selected. A more precise, vehicle-specific overweight check happens later in the pipeline (see #22's sibling behavior), so a value between roughly 34,000kg and 56,000kg may pass this check but still fail later for a specific, smaller vehicle.

---

## C. Downstream pipeline errors (expect HTTP 422)

**Purpose for this section**: these errors happen *after* input validation passes — the request is well-formed and internally consistent, but something about matching it against actual operational data (routes, vehicles, rates) fails. This distinction matters: a validation error means the caller sent something wrong; these errors mean the system couldn't find data to complete an otherwise-legitimate request.

### 20. Unknown location ID

Set `origin_location_id` to a random UUID not in the system, e.g. `99999999-0000-0000-0000-000000000000`.
**Expect**: 422, `code: "UNKNOWN_LOCATION"`.

### 21. No distance available for a lane

Route from Limpopo Rural to Harare via Beit Bridge — this specific combination has no active distance data seeded.
**Expect**: 422, `code: "DISTANCE_NOT_FOUND"`.

### 22. No eligible vehicle for the cargo

Domestic JHB to BFN with `gross_weight_kg: 50000, volume_cbm: 200` — exceeds every seeded vehicle's capacity on that lane.
**Expect**: 422, `code: "NO_ELIGIBLE_VEHICLE"`. This is the precise, vehicle-aware version of the coarse check in #19 — this is the error you'd expect to see for a weight that's simply too large for what's actually available on this specific lane.

### 23. Ambiguous rate configuration (requires V24 fixtures)

Cross-border JHB to Harare via Beit Bridge, `load_type: "reefer"`, `temperature_range_c: {"min": 2, "max": 8}`, `dedicated_vehicle: true`.
**Expect**: 422, `code: "AMBIGUOUS_RATE_CONFIGURATION"`. This represents a data-configuration problem, not a caller error — it means two conflicting rate rows exist for the same lane/vehicle/load-type combination with no rule to pick between them. In production this would point at a rate-table setup mistake, not a request to fix.

Verified end-to-end against the running app (2026-07-05): `{"status":"error","errors":[{"field":"_pipeline","code":"AMBIGUOUS_RATE_CONFIGURATION","message":"Ambiguous rate configuration for lane JHB_METRO:HARARE, vehicle_category=AMBIGUOUS_TEST_VEHICLE, load_type=reefer: [ConflictingRow[id=...,rateBasis=PER_TON], ConflictingRow[id=...,rateBasis=PER_CBM]]"}]}`, HTTP 422 — exact match. Note `dedicated_vehicle: true` is required here, not incidental: the default cost-efficient path's own rate lookup would itself throw before Stage 6 could report the ambiguity — see `PipelineOrchestrationServiceTest`'s equivalent test for the full reasoning.

---

## D. HTTP-level and malformed request handling

**Purpose for this section**: distinguishes "the request is valid JSON but fails business rules" (section B, HTTP 422) from "the request isn't even parseable as the expected shape" (this section, HTTP 400). These are different failure categories and should never be confused with each other in a bug report.

### 24. Malformed JSON

```bash
curl -X POST http://localhost:8080/api/v1/rate-engine/compute \
  -H "Content-Type: application/json" \
  -d '{ "this is not valid json" '
```

**Expect**: HTTP 400 (not 422, not 500).

### 25. Invalid enum value

Take scenario #2's body, set `"route_type": "not_a_real_type"`.
**Expect**: HTTP 400.

### 26. Missing required field entirely (not just null) — a real 500, tracked as a known gap

Take scenario #2's body and remove the entire `cargo` object.

**Expect**: HTTP 500 — **not** 400, **not** 422. Verified against the running app (2026-07-05): `{"status":500,"message":"An unexpected error occurred",...}`. Root cause (visible in server logs after adding logging to the catch-all handler): a raw, uncaught `NullPointerException` — `Cannot invoke "CargoRequest.cargoClass()" because "cargo" is null`. This happens because `PipelineOrchestrationService.compute()`'s own try/catch only wraps the pipeline stages *after* Stage 1 validation — the `inputValidationService.validate(request)` call itself is outside it, so a null-field NPE thrown from inside validation (e.g. `LegalLimitsChecker` computing chargeable weight) is never translated into a proper `PipelineValidationException`/422, and falls through to the generic 500 handler instead. This is a genuine, currently-untreated gap, not a documentation error — tracked as the `rate_endpoint_missing_field_500_deferred` project memory, deliberately not fixed yet (flag to your dev team as a real finding, not a false positive, but don't file it as a *new* bug — it's already known).

### 27. Missing Content-Type header — now fixed, confirmed 415

Repeat scenario #2 without the `-H "Content-Type: application/json"` header.

**Expect**: HTTP 415 (Unsupported Media Type). This was genuinely broken until 2026-07-05 (curl defaults to `application/x-www-form-urlencoded` when `-d` is used without an explicit `Content-Type`, and the server's own broad catch-all exception handler was swallowing the resulting `HttpMediaTypeNotSupportedException` and returning a misleading 500 instead of its natural 415). Fixed by adding a dedicated handler for that exception type — verified against the running app: `{"status":415,"message":"Content-Type 'application/x-www-form-urlencoded;charset=UTF-8' is not supported",...}`.

### 28. Empty request body

```bash
curl -X POST http://localhost:8080/api/v1/rate-engine/compute -H "Content-Type: application/json" -d ''
```

**Expect**: HTTP 400.

### 29. Wrong HTTP method

```bash
curl -X GET http://localhost:8080/api/v1/rate-engine/compute
```

**Expect**: HTTP 405 (Method Not Allowed). Same underlying fix as #27 was needed here too — `HttpRequestMethodNotSupportedException` was also being swallowed by the same broad catch-all into a 500 before 2026-07-05. Verified against the running app: `{"status":405,"message":"Request method 'GET' is not supported",...}`.

### 30. Response content-type sanity check

Repeat scenario #1 or #2, inspect response headers.
**Expect**: `Content-Type: application/json`.

---

## E. Additional pricing-capability scenarios

**Purpose for this section**: section A covered the main pipeline flow; these ten dig into specific pricing rules that are easy to get wrong and worth your team understanding individually rather than only in combination.

### 31. High-value insurance levy — percentage of declared value, not of freight cost

**Purpose**: most percentage-based surcharges are calculated against the base freight amount. This one is a deliberate exception — it's a percentage of the cargo's *declared value*, which is an entirely different number that could be far larger or smaller than the freight cost itself. This scenario isolates that distinction.

Same route as #2, add `high_value_declared: true, declared_value_zar: 500000.00`.
**Expect**: HTTP 200, a `HIGH_VALUE_INSURANCE_LEVY` line item whose amount is a percentage of 500,000 — not related to the `BASE_FREIGHT` line item's value at all.

### 32. Live animals — two separate compliance line items, not one

**Purpose**: shows that a single trigger condition (`live_animals: true`) can produce multiple distinct charges, each representing a genuinely different real-world requirement (animal welfare compliance and vehicle certification are separate concerns, separately charged).

Same route as #2, set `live_animals: true`.
**Expect**: HTTP 200, two separate line items: `LIVE_ANIMAL_WELFARE` and `LIVESTOCK_VEHICLE_CERT`.

### 33. Reefer surcharge triggered by temperature alone, without a reefer load type

**Purpose**: the reefer-running surcharge is designed to trigger under *either* of two independent conditions — the load type is explicitly reefer, or a temperature range is specified regardless of load type (covering a case like a temperature-sensitive item shipped on a non-standard vehicle type). This scenario proves the temperature-alone path works on its own.

Same route as #2, keep `load_type: "ftl"` (not reefer), add `temperature_range_c: {"min": 2, "max": 8}`.
**Expect**: HTTP 200, `REEFER_RUNNING` line item present despite the load type not being reefer.

### 34. Frozen goods — a surcharge calculated on top of another surcharge, not on base freight

**Purpose**: this is the one deliberate exception to the rule that all surcharges are percentages of base freight. When cargo is sub-zero, an additional uplift applies specifically on top of the reefer-running charge's own amount — meaning this line item's value depends on another line item's value, not on `BASE_FREIGHT` directly. Worth your team specifically verifying the math is against the right base.

Same route as #2, `load_type: "reefer"`, `temperature_range_c: {"min": -18, "max": -10}`.
**Expect**: HTTP 200, both `REEFER_RUNNING` and `FROZEN_GOODS_UPLIFT` present, with the latter calculated as a percentage of the former's amount, not of `BASE_FREIGHT`.

### 35. Non-stackable LTL cargo — a surcharge that only applies to one specific load type

**Purpose**: some surcharges are conditional not just on a cargo attribute but on a *combination* of attributes — this one only applies when cargo is both non-stackable AND being shipped LTL (less-than-truckload, shared vehicle space). The same non-stackable flag on an FTL (full truckload) shipment should produce no such charge, since the rule only makes sense in a shared-space context.

Route JHB to BFN, `load_type: "ltl"`, `pallet_count: 4`, `stackable: false`.
**Expect**: HTTP 200, `NON_STACKABLE_SPACE_FACTOR` line item present. (For comparison, try the same request with `load_type: "ftl"` and no pallet count — the surcharge should NOT appear even with `stackable: false`, since it's LTL-specific.)

### 36. A shipment small enough to hit the minimum charge floor

**Purpose**: every rate lane has a minimum charge — if the computed freight cost for a very small or short shipment comes out below that floor, the engine charges the floor amount instead, with a specific audit comment recorded on the line item. This scenario is designed to be small enough to actually hit that floor, so your team can see the flooring behavior directly rather than take its existence on faith.

Route JHB to BFN, `gross_weight_kg: 50, volume_cbm: 0.1` (a very small shipment).
**Expect**: HTTP 200, `BASE_FREIGHT` line item includes a comment `"Minimum charge applied."` and its value equals the lane's configured minimum charge, not the (much smaller) raw computed value.

### 37. Express service level — a materially different multiplier than Standard

**Purpose**: the engine supports four service tiers (Economy 1.00×, Standard 1.15×, Express 1.40×, Dedicated 1.65×), each applying a different uplift to the pre-multiplier subtotal. This scenario, compared directly against scenario #2 (Standard), lets your team see the dollar impact of tier selection concretely rather than just trusting a documented multiplier table.

Same as scenario #2, but `service_level: "express"`.
**Expect**: HTTP 200, `SERVICE_MULTIPLIER` line item's value is proportionally larger than scenario #2's — confirm it reflects exactly 1.40× against the pre-multiplier subtotal, and that `BASE_FREIGHT`/`FUEL_LEVY` themselves are unchanged from #2 (only the multiplier line item and the final total should differ).

### 38. All four accessorial charges at once, each billed independently per end

**Purpose**: accessorial charges (after-hours, tail lift, driver assist) are billed per specific end of the journey — collection or delivery — not as one lump "accessorial required" charge. This scenario sets every flag simultaneously to confirm each produces its own distinct, correctly-priced line item, and that setting a collection-side flag doesn't also silently charge for delivery, or vice versa.

Same route as #2, in the `service` object set `after_hours_collection: true, after_hours_delivery: true, tail_lift_collection: true, tail_lift_delivery: false, driver_assist_loading: true, driver_assist_offloading: false`.
**Expect**: HTTP 200, line items for `AFTER_HOURS_COLLECTION`, `AFTER_HOURS_DELIVERY`, `TAIL_LIFT_COLLECTION`, and `DRIVER_ASSIST_LOADING` — but explicitly **not** `TAIL_LIFT_DELIVERY` or `DRIVER_ASSIST_OFFLOADING`, since those flags were left `false`. These charges are also **not** multiplied by the service-level uplift — confirm the accessorial amounts match their flat seeded rates exactly, unaffected by whatever `service_level` is set.

### 39. VAT is always charged — zero-rating exists in the code but can never actually trigger today

**Purpose**: similar in spirit to scenario #4 — this is a known, deliberate current limitation worth your team understanding rather than discovering by accident. The tax engine has zero-rating logic built in for export-classified shipments (which should be VAT-exempt), but there's currently no field anywhere in the request that lets a caller indicate export status. So today, VAT is calculated and added on literally every request, with no way to trigger the exemption. This isn't a bug to file — it's a known gap tracked separately, pending a business decision on how export status should be declared.

Any valid request, e.g. scenario #2 as-is.
**Expect**: HTTP 200, `totals.vat_zar` is always a positive, non-zero value — try to find any combination of flags that makes it zero (there isn't one today), so your team has direct confirmation of this rather than relying on this note alone.

### 40. Identical requests produce identical prices — but not identical response IDs

**Purpose**: this is a foundational design principle worth your team verifying directly: the engine is meant to be perfectly deterministic for pricing — same input, same rate table version, same price, every time, which matters enormously for audit and dispute-resolution purposes. But two *separate calls*, even with identical input, are still treated as two distinct computations for tracking purposes, so each gets its own unique snapshot identifier. This scenario asks your team to run the exact same request twice and compare the two responses field by field.

Run scenario #2 twice, back to back, with the same `quote_context_id` both times.
**Expect**: both calls return HTTP 200 with identical `totals`, identical `line_items`, identical `vehicle_selected`, identical `distance_km` — but a **different** `rate_snapshot_id` and `computed_at` timestamp on each call. If any of the pricing fields differ between the two calls, that's a serious finding worth escalating immediately, since it would mean the engine isn't actually deterministic.

---

## Suggested test-team workflow

1. **Run scenarios 1, 2, and the "how the engine works" summary above first** — get comfortable with the full response shape and the pipeline's stage order before touching anything else.
2. **Run 3 through 8 and 31 through 40 next** — these teach the individual pricing rules and capabilities one at a time. Understanding these deeply is what will let your team start noticing *combinations* worth probing that aren't explicitly in this catalog.
3. **Run 9 through 23 as a batch** — all error-path scenarios, good candidates for an automated pass/fail script checking the exact error code returned.
4. **Run 24 through 30 to understand the boundary between "malformed request" and "valid request that fails business rules.**"
5. **Flag scenarios 4, 5, 6, 23, 26, and 39 explicitly in any test report** — these are either known, deliberate current limitations, or dependent on test-only seed data. Treat unexpected deviations from what's documented here as worth investigating, but treat the documented behavior itself as expected, not a defect. Scenario 26 specifically is a **confirmed 500** (an uncaught null-pointer gap, tracked but not yet fixed) — worth a bug-tracking entry referencing the existing known gap, not a fresh investigation.
6. **27 and 29 were genuinely open questions as of the catalog's first draft, resolved on 2026-07-05**: both were found to return an incorrect 500 (a too-broad exception handler swallowing well-understood Spring MVC exceptions) and have since been fixed to return their correct statuses (415 and 405 respectively). If your environment predates that fix, you'll see 500 instead — worth confirming which build you're testing against.

If it would help, this can also be built as an importable Postman collection instead of, or alongside, curl commands, so your team can run and organize these as a saved, shareable collection.
