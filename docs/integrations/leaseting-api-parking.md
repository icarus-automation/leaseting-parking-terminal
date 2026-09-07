# The gate loop against leaseting-api

Everything the terminal does after sign-in: opening a shift, printing a stub,
pricing a stay, taking the cash, handing the till over. This is Parking Phase
2A, which is the terminal half. The admin half — voiding a collected exit,
confirming a declared shift — is Phase 2B and is specified at the end.

Sign-in itself is [`leaseting-api-auth.md`](leaseting-api-auth.md).

## The rule the whole design turns on

**The terminal never computes money.**

Staff set rate plans, tiers, overnight windows, grace and rounding in the web
client, and they change them whenever they like. A handheld that priced stays
from a cached rate board would keep collecting yesterday's figure until someone
noticed. So the API prices every stay, at the moment of the exit, and the
terminal shows that number and posts cash against it.

There is no fee field in any request the terminal sends. There is no waive, no
override and no discount on the device. A fee that is genuinely wrong is an
admin void in the web client.

The consequence is that Phase 2A is online-first: an exit needs the API. The
offline queue in the PRD builds on this once the online loop is proven, and it
queues *entries* — which cost nothing and can be reconciled — rather than fees.

## Audience and scope

Every route below is on `ParkingOperationsController` (`@Controller('parking')`)
and carries `@AllowAudience('parking')`, which opts it past `AudienceGuard`'s
default-deny for non-staff roles. The organization comes from the session's
active organization through `TenantPrismaService`, and RLS keeps it there.

The attendant is always `session.user.id`. It is never accepted from a body or
a query, so a handheld can only ever act as whoever is signed in on it.

Responses use the standard `{ statusCode, message, data }` envelope. Rejections
use `HttpExceptionFilter`'s `{ statusCode, message, error, path, timestamp }`,
where `message` is a sentence for a domain error and an array for a validation
failure. The terminal reads that message and shows it: the API knows things the
handheld does not, and "Hourly has no price for Truck" is worth more at a
barrier than any wording the terminal could invent.

## Shifts

A shift is one guard's turn at one terminal, and the till that goes with it.
Nothing can be recorded without one.

### `POST /api/v1/parking/shifts`

```json
{ "terminalId": "<uuid>" }
```

Opens a shift, or hands back the one the caller already holds on that terminal —
a handheld that restarts mid-shift comes back to the same till rather than
opening a second one. Another attendant's open shift is `409`.

A disabled terminal is `400`; an unknown one is `404`.

Only one shift per terminal may be `OPEN`. The service checks it, and the
partial unique index `parking_shifts_one_open_per_terminal` in
`prisma/rls/tenant-policies.sql` makes it unraceable.

### `GET /api/v1/parking/shifts/current`

`data` is `null` when the attendant holds no open shift — the normal state right
after sign-in, not an error. The terminal calls this on every launch.

### `GET /api/v1/parking/shifts/{id}/summary`

```json
{
  "shiftId": "<uuid>",
  "status": "OPEN",
  "openedAt": "2026-09-07T06:00:00.000Z",
  "entryCount": 24,
  "paidExitCount": 19,
  "expectedCash": "1850.00"
}
```

`expectedCash` is the sum of `feeAmount` over sessions **exited** on this shift,
not entered on it: a vehicle that came in on the morning shift and left on the
afternoon one puts its money in the afternoon till. A declared shift reports the
figure that was snapshotted at declare rather than recomputing.

Someone else's shift answers `404`, not `403`. The handheld has no business
learning that another attendant's till exists.

### `POST /api/v1/parking/shifts/{id}/declare`

```json
{ "declaredCash": 1830, "note": "short by 20" }
```

Ends the shift. The API computes `expectedCash` itself and writes it alongside
the guard's count, so an admin reconciling later compares against the number the
guard was shown rather than one a subsequent void or reprice would produce.

Declaring is one-way and idempotent-by-conflict: the update is conditional on
`status = OPEN`, so two taps on "End shift" cannot overwrite the first count
with a second. A shift is never reopened; the next turn is a new one.

## Sessions

### `POST /api/v1/parking/sessions`

```json
{ "shiftId": "<uuid>", "vehicleTypeId": "<uuid>", "plateNumber": "ABC 1234" }
```

Opens a stay and mints its `ticketCode`. The terminal, the attendant and the
entry time all come from the shift and the clock, so a stub cannot be back-dated
or attributed elsewhere.

`ticketCode` is eight characters from `23456789ABCDEFGHJKMNPQRSTUVWXYZ` — no
`0`/`O`, no `1`/`I`/`L`, because a guard reads it back off 58 mm thermal paper
when the QR will not scan. It is unique per organization; a collision is
re-rolled rather than surfaced as a 500 at a barrier.

### `GET /api/v1/parking/sessions?terminalId=&plate=&status=`

Defaults to `status=OPEN` across every terminal in the organization, oldest
first. The terminal scopes the in-park list to the gate being worked, and
deliberately does *not* scope a plate search: a stub lost on a vehicle that
entered by another gate still has to be findable, so each row carries the
terminal it entered at.

### `GET /api/v1/parking/sessions/by-code/{code}`

The scan path. The code is normalised the same way on both sides — upper-cased,
non-alphanumerics stripped — so a wedge scanner's trailing newline and the dash
a guard copies off the stub both work.

Answers for a stub in **any** state. A code that has already been paid comes
back as `PAID` rather than `404`, because scanning the same stub twice is the
commonest mis-scan at a gate and it deserves a plain answer.

### `GET /api/v1/parking/sessions/{id}/fee-preview?ratePlanId=`

```json
{
  "sessionId": "<uuid>",
  "ticketCode": "A3KD9MPQ",
  "plateNumber": "ABC 1234",
  "vehicleTypeId": "<uuid>",
  "vehicleTypeName": "Car",
  "entryAt": "2026-09-07T02:00:00.000Z",
  "quotedAt": "2026-09-07T04:30:00.000Z",
  "stayMinutes": 150,
  "billedMinutes": 135,
  "graceMinutes": 15,
  "ratePlanId": "<uuid>",
  "ratePlanName": "Hourly",
  "feeAmount": "150.00"
}
```

Priced as of now, against `parkingRules.defaultTransientRatePlanId` unless
`ratePlanId` overrides it. Nothing is stored.

The formula is `computeParkingFee` in `src/common/utils/parking-fee.util.ts`,
which is the same function the exit uses:

1. Stay at or under `graceMinutes` is free. Billable time starts after grace.
2. If the plan has a clock window (`windowStartMinute`/`windowEndMinute`, Manila
   wall-clock, start > end spans midnight), split billable minutes into
   in-window and outside.
3. In-window (or the whole stay with no window): snap onto
   `plan.incrementMinutes` with `roundingMode`, consume `durationTiers` in order
   (each band is flat for any occupancy of it), then leftover units ×
   `amounts[vehicleTypeId]`.
4. Outside the window: snap onto 60 minutes, × `overtimeAmounts[vehicleTypeId]`.
5. A missing succeeding amount means the type is not sold on that plan, and the
   API answers `400` naming the plan and the type rather than charging zero.

`lostTicketFee` stays unused. A lost stub is a plate lookup and the ordinary
duration fee — no surcharge.

### `POST /api/v1/parking/sessions/{id}/exit`

```json
{ "shiftId": "<uuid>", "amountTendered": 200, "ratePlanId": "<uuid>" }
```

Prices the stay again — the preview the guard saw is a second old at best — and
rejects a tender that does not cover it (`400`, naming the figure). On success
it writes the fee, the tender, the change and a **snapshot of the plan id and
name and the billed minutes**, because an admin repricing that plan tomorrow
must not move what was collected today.

The write is conditional on `status = OPEN`, so a double tap or a second
handheld reaching the same stub collects once and the loser gets `409`.

Cash only in v1: there is no method field.

## Phase 2B: the admin contract

Not implemented. The tables already carry the columns, so 2B is routes and
guards rather than a migration.

### `POST /api/v1/parking/sessions/{id}/void`

Staff only — `@OrgRoles(['owner', 'admin'])`, no `@AllowAudience`, so the
terminal cannot reach it.

```json
{ "reason": "Overcharged, refunded in cash" }
```

Moves a `PAID` session to `VOID` and writes `voidedAt` and `voidedReason`. This
is the *only* correction path for a collected exit, which is what lets the
terminal treat a paid stay as immutable.

A void must not silently change a shift that has already been declared: the
declared `expectedCash` is a snapshot precisely so an admin can see that a
reconciled figure included money that was later voided. Reconciliation reporting
should therefore read voids separately rather than re-summing sessions.

`ParkingSession.exitShiftId` is `onDelete: Restrict`, so a voided session keeps
pointing at the shift that collected it.

### `POST /api/v1/parking/shifts/{id}/confirm`

Staff only. Moves `DECLARED` to `CONFIRMED` and writes `confirmedAt` and
`confirmedById` (both already on the model). Takes an optional note; the
recounted figure, if an admin wants one, is a decision for 2B — the column set
today records who confirmed and when, not a third cash figure.

A shift that is not `DECLARED` is `409`. Confirming is one-way.

### `GET /api/v1/parking-shifts`

Staff-side list for reconciliation: filter by property, terminal, attendant,
status and date range; return `expectedCash`, `declaredCash` and their variance.
This is what the web client's parking reconciliation screen reads.

## Schema

Two tables, both org-scoped and both under RLS
(`prisma/rls/tenant-policies.sql`, plus the migration
`20260907101500_parking_shifts_and_sessions`).

`parking_shifts` — status, `openedAt`/`declaredAt`/`confirmedAt`,
`expectedCash`/`declaredCash`, note, and FKs to terminal, attendant and the
confirming admin.

`parking_sessions` — `ticketCode`, `plateNumber`, status, `entryAt`/`exitAt`,
the priced-at-exit snapshot (`ratePlanId`, `ratePlanName`, `billedMinutes`,
`feeAmount`, `amountTendered`, `changeGiven`), the 2B void columns, and FKs to
terminal, vehicle type, rate plan, and the entry and exit shift and attendant.

Entry and exit are recorded separately on purpose. A vehicle can enter at one
gate on one shift and leave at another gate on another, and both halves have to
be attributable.
