# Parking terminal attendant display name

Use the fields below for every screen or 58 mm receipt line labeled **Attendant**. Do not print `role`, `organizationRole`, `name`, an email address, or a hard-coded role label.

All paths use the `/api/v1` prefix and require the terminal's authenticated session.

## Signed-in attendant

`GET /api/v1/users/me`

`displayName` is present for every authenticated user. For the parking terminal, accept the identity only when `organizationRole` is `parking_attendant` and `audience` is `parking`.

```json
{
  "id": "8ea440ae-2f17-48c6-a29d-a30d1ca81eec",
  "name": "Ana Reyes",
  "displayName": "Ana Reyes",
  "email": "parkingattendant@leaseting.com",
  "image": null,
  "role": "user",
  "organizationRole": "parking_attendant",
  "audience": "parking",
  "features": {
    "sms": false
  }
}
```

Print `/users/me.displayName` in the gate header or signed-in attendant label.

## Entry stub

`POST /api/v1/parking/sessions`

A successful response always includes `entryAttendantDisplayName`. `exitAttendantDisplayName` is `null` while the parking session is open.

```json
{
  "id": "4014e947-6166-4a29-bde5-5a661997f7d2",
  "ticketCode": "A3KD9MPQ",
  "plateNumber": "ABC 1234",
  "status": "OPEN",
  "entryAt": "2026-09-09T01:20:00.000Z",
  "exitAt": null,
  "terminalId": "1d5efef9-880a-4e38-9a66-3c3dc59ac344",
  "terminalName": "Basement Gate",
  "vehicleTypeId": "7eec3942-82af-42d4-b2d9-c03d5fa07de3",
  "vehicleTypeName": "Car",
  "ratePlanId": null,
  "ratePlanName": null,
  "billedMinutes": null,
  "feeAmount": null,
  "amountTendered": null,
  "changeGiven": null,
  "entryShiftId": "e064b93a-e56e-4659-976a-0c857943cbdf",
  "entryAttendantDisplayName": "Ana Reyes",
  "exitShiftId": null,
  "exitAttendantDisplayName": null
}
```

Print `entryAttendantDisplayName` on the entry stub's Attendant line.

## Paid exit receipt

`POST /api/v1/parking/sessions/:id/exit`

A successful paid response includes both names. `exitAttendantDisplayName` is the person who collected the payment, even when a different person created the entry.

```json
{
  "id": "4014e947-6166-4a29-bde5-5a661997f7d2",
  "ticketCode": "A3KD9MPQ",
  "plateNumber": "ABC 1234",
  "status": "PAID",
  "entryAt": "2026-09-09T01:20:00.000Z",
  "exitAt": "2026-09-09T03:45:00.000Z",
  "terminalId": "1d5efef9-880a-4e38-9a66-3c3dc59ac344",
  "terminalName": "Basement Gate",
  "vehicleTypeId": "7eec3942-82af-42d4-b2d9-c03d5fa07de3",
  "vehicleTypeName": "Car",
  "ratePlanId": "9a750cf2-d4f9-4d8c-a547-a788d087984f",
  "ratePlanName": "Hourly",
  "billedMinutes": 130,
  "feeAmount": "150.00",
  "amountTendered": "200.00",
  "changeGiven": "50.00",
  "entryShiftId": "e064b93a-e56e-4659-976a-0c857943cbdf",
  "entryAttendantDisplayName": "Ana Reyes",
  "exitShiftId": "513060b3-41b0-4277-a7e7-19af0732749f",
  "exitAttendantDisplayName": "Rico Delgado"
}
```

Print `exitAttendantDisplayName` on the paid exit receipt's Attendant line.

## Other session reads

These endpoints return the same attendant fields:

- `GET /api/v1/parking/sessions`
- `GET /api/v1/parking/sessions/by-code/:code`

For an `OPEN` session, `exitAttendantDisplayName` is `null`. For a successfully paid session, it is the collecting attendant's person name.
