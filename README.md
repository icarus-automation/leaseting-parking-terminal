# Leaseting Parking Terminal

Native Android parking terminal for the Leaseting Property Management System.

It runs on the H10S handheld, is operated by a guard at the gate, and prints on
the device's integrated 58 mm thermal printer. It is a separate application from
the resident mobile app and the web administration client.

## Stack

Kotlin, Jetpack Compose with Material 3, ViewModel and StateFlow, AndroidX
Navigation, Retrofit and OkHttp against `leaseting-api`, DataStore with
Android Keystore encryption for the session, and the vendor `printer.jar` for
the integrated printer.

Light theme only, no dynamic color. Every color literal lives in
`ui/theme/Color.kt`; screens never hardcode one.

## Requirements

- Android Studio (the version that ships the AGP this project pins)
- JDK 21
- A running [`leaseting-api`](https://github.com/icarus-automation) instance
- minSdk 24, targetSdk 36

## Running it

Release builds point at production. Debug builds default to
`http://10.0.2.2:8000/api/v1/`, which is the emulator's route to the host
machine and means nothing on a physical device, so override it in
`local.properties` (untracked):

```properties
leaseting.api.baseUrl=http://localhost:8000/api/v1/
```

`API_BASE_URL` is baked into `BuildConfig` at compile time, so changing that
file means rebuilding and reinstalling.

On a real handheld over USB, forward the device's port 8000 back to your
machine and keep the `localhost` URL above:

```bash
adb reverse tcp:8000 tcp:8000
```

`adb reverse` has to be re-run whenever the device reconnects. Cleartext HTTP is
permitted in debug builds only.

```bash
./gradlew testDebugUnitTest   # unit tests
./gradlew assembleDebug       # debug APK
```

## Signing in

The terminal accepts one kind of account: an organization member whose role is
`parking_attendant`. Any other account is rejected and its token revoked before
anything is written to the device, so a staff or resident credential never lands
on a handheld. Staff create and manage these logins in the web client under
Settings > Parking Management > Parking attendants.

Full flow, session storage, and session lifetime:
[`docs/integrations/leaseting-api-auth.md`](docs/integrations/leaseting-api-auth.md).

## Documentation

| File | What it covers |
| --- | --- |
| [`AGENTS.md`](AGENTS.md) | Repository conventions and hard rules |
| [`docs/PRD.md`](docs/PRD.md) | What the application must do |
| [`docs/integrations/leaseting-api-auth.md`](docs/integrations/leaseting-api-auth.md) | Authentication against `leaseting-api` |
| [`docs/integrations/h10s-printer.md`](docs/integrations/h10s-printer.md) | Talking to the handheld's printer |

`app/libs/printer.jar` is the vendor SDK and the binary source of truth for
printer APIs. Its package spelling `recieptservice.com.recieptservice` is the
vendor's and must not be corrected.

## Related repositories

- `leaseting-api` — NestJS backend and PostgreSQL system of record
- `leaseting-client` — web administration and reports
- `leaseting-mobile` — Residence Care resident app
