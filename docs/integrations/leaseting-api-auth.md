# Authentication against leaseting-api

How the terminal signs a guard in, and why only one kind of account gets through.

## The `parking_attendant` role

Leaseting accounts carry an organization role on `members.role`. The roles and
the application each one belongs to are defined in one place in the backend,
`src/core/auth/organization-roles.ts`:

| Role                | Audience  | Application               |
| ------------------- | --------- | ------------------------- |
| `owner`             | `staff`   | leaseting-client (web)    |
| `admin`             | `staff`   | leaseting-client (web)    |
| `member`            | `staff`   | leaseting-client (web)    |
| `tenant`            | `tenant`  | Residence Care (mobile)   |
| `parking_attendant` | `parking` | **This terminal**         |

`parking_attendant` has no organization permissions. `AudienceGuard` in the
backend default-denies it from every staff endpoint, and leaseting-client
refuses the session at login and signs it back out.

The terminal applies the same rule in reverse: a session whose audience is not
`parking` is rejected and its token revoked, so a staff or resident account can
never leave a credential on the handheld.

## Sign-in flow

Better Auth issues session cookies, which a handheld has no way to keep. The
backend therefore enables Better Auth's `bearer` plugin, and the terminal
authenticates with a token.

1. `POST /api/v1/auth/sign-in/email` with `{ email, password }`.
   The session token comes back in the **`set-auth-token` response header**
   (the response body carries a copy under `token` as a fallback).
2. `GET /api/v1/users/me` with `Authorization: Bearer <token>`.
   Answers with the standard `{ statusCode, message, data }` envelope; `data`
   carries `organizationRole` and `audience`.
3. If `audience` is not `parking`, `POST /api/v1/auth/sign-out` with the same
   bearer header and show the guard why they were turned away. Nothing is
   written to disk.
4. Otherwise store the token and the attendant, and send
   `Authorization: Bearer <token>` on every later call.

The active organization is already on the session: the backend pins it when a
dedicated app role signs in, so the terminal never calls
`organization/set-active`.

## Where the session lives

`EncryptedSessionStore` keeps one DataStore entry, AES/GCM encrypted under a
non-exportable Android Keystore key (`SessionCipher`). It is the only session
state in the app — no in-memory mirror that could disagree with what survives a
restart, and `allowBackup` is off so the credential is not copied off-device.

The session is trusted offline and never revalidated on launch: a gate loses
connectivity, and a guard cannot be asked to sign in again because of it. A
token that has actually been revoked is discovered the next time it is used —
`UnauthorizedInterceptor` clears the session on a 401 to a request that carried
one.

## How long a session lasts

Better Auth sessions expire after 7 days. A terminal session is issued for 30
instead (`PARKING_SESSION_LIFETIME_SECONDS`): the handheld lives at a gate, and
a guard who has to find an admin to get back in is a guard who is not raising
the barrier. Better Auth still extends any session used in the last week of its
life, so a terminal in daily use never signs itself out. One left in a drawer
stops working.

Two things end a session early, both immediate: an admin resetting the
attendant's password, and an admin disabling the attendant.

## Managing terminal logins

Staff manage attendants in the web client under **Settings > Parking
Management > Parking attendants** (owner and admin only). Adding one creates
the user and grants `parking_attendant`.

The admin always types the password, minimum 8 characters. Nothing is
generated: the terminal has no change-password screen, so whatever is set has
to be something a guard can enter on a handheld keypad. Resetting it is the
only way to change it, and doing so signs out every terminal on the old one.

Disabling sets Better Auth's `banned` flag rather than deleting the account, so
the transactions an attendant recorded keep pointing at a real user. Disabled
logins stay in the list.

## Seeded account

`prisma/seeds/prod.ts` in leaseting-api creates the first terminal login:

```
parkingattendant@leaseting.com / parking123
```

Override the password with `SEED_PARKING_PASSWORD` before seeding a real
environment.

## Pointing a debug build at a local API

Release builds use `https://api.leaseting.com/api/v1/`. Debug builds default to
`http://10.0.2.2:8000/api/v1/`, the emulator's route to the host. Override it
per workstation in `local.properties` (untracked):

```
leaseting.api.baseUrl=http://192.168.1.20:8000/api/v1/
```

Cleartext HTTP is permitted in debug builds only, through
`app/src/debug/res/xml/network_security_config.xml`.

### On a real handheld over USB

The simplest route needs no IP and no firewall change. With the device
connected and USB debugging on, forward the device's own port 8000 to the
development machine:

```
adb reverse tcp:8000 tcp:8000
```

Then point the build at the device's localhost:

```
leaseting.api.baseUrl=http://localhost:8000/api/v1/
```

`adb reverse` has to be re-run after the device reconnects.

`API_BASE_URL` is baked into `BuildConfig` at compile time, so changing
`local.properties` means rebuilding and reinstalling. A handheld that says
"Can't reach the server" is usually still running an APK built without the
override, which leaves it pointing at `10.0.2.2` — an address that only means
anything inside an emulator.

### On a real handheld over Wi-Fi

Both machines on the same network. leaseting-api already listens on
`0.0.0.0`, so the only setup is allowing its port through the development
machine's firewall and pointing `leaseting.api.baseUrl` at that machine's LAN
address.
