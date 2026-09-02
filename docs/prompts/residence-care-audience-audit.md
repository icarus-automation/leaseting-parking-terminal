# Audit prompt: Residence Care against the new audience guard

You are auditing the Residence Care mobile app (`leaseting-mobile`) against a
change that just landed in the backend, `leaseting-api`. Your job is to find
every request the app makes that the backend will now reject, and report them.
Do not fix anything yet.

## What changed in leaseting-api

Leaseting accounts carry an organization role on `members.role`. Each role
belongs to exactly one client application:

| Role                | Audience  | Application               |
| ------------------- | --------- | ------------------------- |
| `owner`             | `staff`   | leaseting-client (web)    |
| `admin`             | `staff`   | leaseting-client (web)    |
| `member`            | `staff`   | leaseting-client (web)    |
| `tenant`            | `tenant`  | **Residence Care**        |
| `parking_attendant` | `parking` | Leaseting Parking Terminal |

A global Nest guard (`AudienceGuard`) now default-denies any non-staff session
from any endpoint that has not explicitly opted that audience in. A rejected
request answers **403** with the message `Use the Residence Care mobile app.`

This guard existed before under a different name, but it was registered in a
position where it ran **before** the Better Auth guard had attached
`request.session`. It read an undefined session, took that as "no membership,
treat as staff", and allowed everything. That ordering bug is fixed, so the
rule is enforced for the first time. **Calls that used to succeed by accident
will now 403.**

## What a `tenant` session is still allowed to call

Everything else is blocked.

1. Better Auth routes: anything under `/api/v1/auth/**` (sign-in, sign-out,
   session, organization). Not covered by the guard at all.
2. `GET /api/v1/users/me`
3. Every route under `/api/v1/portal/**`

Note that `/api/v1/users/me` now also returns an `audience` field
(`'staff' | 'tenant' | 'parking'`) alongside the existing `organizationRole`.
That is additive. Nothing in the app has to read it.

Sign-in itself is unchanged. The backend added Better Auth's `bearer` plugin
for the Android parking terminal, which makes sign-in *also* return a
`set-auth-token` header. The `expo` plugin and its cookie handling are
untouched, so the mobile session flow needs no change.

## Your task

1. **Inventory every HTTP call the app makes to leaseting-api.** Search the
   whole repo, not just an `api/` folder. Cover:
   - fetch / axios / ky / whatever client wrapper this repo uses
   - React Query / SWR / TanStack hooks and their query functions
   - any base-URL constant plus a path concatenated onto it
   - paths built from template literals or variables (resolve them by hand)
   - Better Auth client calls (`authClient.*`)
   - background tasks, push-notification handlers, deep-link handlers, and
     anything that runs on app start before a screen mounts
2. **Normalize each one to a full path** beginning `/api/v1/`. Watch for a base
   URL that already includes `/api/v1`.
3. **Classify** each path as ALLOWED or BLOCKED using the list above.
4. For every BLOCKED call, record:
   - `file:line`
   - HTTP method and full path
   - the user-facing feature that triggers it
   - whether the code is actually reachable, or dead / commented out / behind a
     disabled flag
5. **Verify the ones you are unsure about against a running API** rather than
   guessing. With leaseting-api running on port 8000 and its seed applied:

   ```bash
   curl -s -c jar.txt -H 'Content-Type: application/json' \
     -d '{"email":"juan.delacruz@gmail.com","password":"juan101rent"}' \
     http://localhost:8000/api/v1/auth/sign-in/email

   # 200 = allowed, 403 = blocked by the audience guard
   curl -s -b jar.txt -o /dev/null -w '%{http_code}\n' \
     http://localhost:8000/api/v1/<the path you are checking>
   ```

   `juan.delacruz@gmail.com` / `juan101rent` is a seeded tenant. If the seed
   differs in your environment, find a user whose `members.role` is `tenant`.

## What to report

A single table, most severe first, plus a one-line verdict.

```
| Feature | Method + path | file:line | Reachable | Verdict |
```

Then say plainly: **does Residence Care break, and where.** If nothing is
blocked, say so and state how many call sites you checked.

## If you do find blocked calls

Report them and stop. Do not edit leaseting-api. Two fixes exist, and choosing
between them is a decision for the maintainer, not for you:

- **Preferred:** move the data behind a `/portal` endpoint. `/portal` is the
  tenant-facing surface and is already scoped to the signed-in tenant, so a
  tenant cannot read another tenant's rows through it.
- **Only when the endpoint is genuinely tenant-safe as written:** add
  `@AllowAudience('tenant')` to that specific handler in leaseting-api. This
  is a real widening of a security boundary. It requires checking that the
  handler scopes its query to the caller's own tenant, not merely to the
  organization. Most staff endpoints scope to the organization, which means a
  tenant would be able to read every other tenant's data. Assume the endpoint
  is unsafe until you have read the query and confirmed otherwise.

Include, for each blocked call, which of the two you would recommend and the
one-sentence reason.
