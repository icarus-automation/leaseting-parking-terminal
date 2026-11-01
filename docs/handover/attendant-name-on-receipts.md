# Attendant name on parking receipts

**Status:** the terminal side is done. What remains is one data check per
account, described under "What still needs doing".

**Affects:** the printed exit receipt, the printed entry stub, and the
"Attendant:" line on the gate screen.

**Contract:** `docs/integrations/attendant-display-name.md`.

## What the terminal prints now

Every Attendant line takes a person name from the API. Nothing is substituted on
the device, and no role label is printed anywhere.

| Line | Field |
| --- | --- |
| Gate screen, "Attendant:" | `GET /users/me` &rarr; `displayName` |
| Entry stub, "Attendant" | `POST /parking/sessions` &rarr; `entryAttendantDisplayName` |
| Exit receipt, "Attendant" | `POST /parking/sessions/:id/exit` &rarr; `exitAttendantDisplayName` |

The receipt names whoever the API recorded as collecting, not whoever is holding
the handheld. Those differ when one guard books a vehicle in and another
collects on the way out, which is what a shift change mid-stay looks like on
paper, and the receipt shows the second person.

Where the API sends no `displayName` at all, the terminal falls back to the
account's `name`. That is the only fallback, and it exists for a build of the
API from before the field.

## What still needs doing

The terminal prints whatever name the account carries. If an account's
`displayName` is still a role rather than a person, the receipt will faithfully
print the role.

**Check each parking account's display name in the Leaseting web client.** An
account whose display name reads "Parking Attendant" is the thing to fix, and
fixing it needs no rebuild and no reinstall: the next sign-in picks up the new
name.

| Step | Owner |
| --- | --- |
| Decide whether guards get individual logins | Client |
| Give each parking account a person's display name | Client, in the web client |
| Hand each guard their own credentials | Client |
| Stop using any shared login | Client |
| Verify a receipt prints the person's name | Anyone, next paid exit |

## Testing notes

**The shared account is obvious when it is still in use.** If the receipt or the
gate header reads:

```text
Attendant      Parking Attendant
```

then that account's `displayName` is still the role. This is not a terminal
bug and there is nothing to fix in the app. Check `GET /users/me` for that
account: whatever `displayName` returns is what the paper will say.

**The gate header is cached until the next sign-in.** The signed-in attendant's
name is stored on the device at sign-in. A handheld that was already signed in
before an account was renamed keeps showing the old name on the gate screen
until someone signs out and back in. The printed lines do not have this problem,
because they come from the API's response to each transaction.

**Long names are handled.** A name too wide for 58 mm paper breaks between its
words onto a second line, never through a word. See
`docs/integrations/h10s-printer.md`, section 8a.

## Why it matters

A cash receipt is the only paper record of the handover between a driver and a
guard, and the shift declaration at the end of the night is reconciled against
it. If every gate on every shift signs in as one shared account:

- Two guards on the same terminal are indistinguishable on paper.
- A disputed collection cannot be traced to a person.
- A cash variance at end of shift cannot be attributed.

## If individual logins are refused

If the client decides one shared login per gate is what they want, the terminal
is doing its job and nothing is broken. Record that decision, because the
tradeoff lands on whoever reconciles a cash variance later.
