# Leaseting Parking Terminal

## Project

This repository contains the native Android parking terminal for Leaseting Property Management System.

It runs on the H10S handheld device and supports guard/gate transactions, offline operation, and integrated 58 mm receipt printing.

This is an operational terminal application. It is separate from the resident mobile app and web administration client.

## Related Repositories

- `leaseting-client`: Web administration and reports
- `leaseting-api`: NestJS backend and PostgreSQL system of record
- `leaseting-mobile`: Residence Care mobile app
- `leaseting-parking-terminal`: This Kotlin Android application 

## Stack

- Kotlin and Android Studio
- Jetpack Compose and Material 3
- ViewModel and StateFlow
- AndroidX Navigation
- Room for local persistence
- WorkManager for reliable background synchronization
- Retrofit and OkHttp for `leaseting-api`
- Kotlin serialization for JSON
- DataStore for non-sensitive preferences
- Android Keystore-backed encryption for long-lived credentials
- Official H10S `printer.jar` for integrated printing

## PRD

Product requirements document are located at:

- `docs/PRD.md`

This explains **what the application must do**.

## Backend Integration

The terminal authenticates against `leaseting-api` with the dedicated
`parking_attendant` organization role. How the role, the bearer-token sign-in
and the on-device session work:

- `docs/integrations/leaseting-api-auth.md`

## H10S Hardware Integration

The application runs on an H10S Android handheld terminal.

The H10S manufacturer SDK is used for device-specific hardware functionality, primarily:

- Built-in 58 mm thermal printer
- NFC, if required
- Barcode/scanner hardware, if required

### SDK References

SDK documentation and integration notes are located at:

- `docs/integrations/h10s-printer.md`

This explains **how the application interacts with the handheld hardware**.

Before implementing or modifying H10S-specific functionality:

1. Read `docs/integrations/h10s-printer.md`.
2. Treat `app/libs/printer.jar` as the binary source of truth.
3. Do not use AAR APIs or add raw AIDL files.
4. Do not invent methods absent from the compiled JAR.

### Printer SDK

The printer SDK binary is located at:

- `app/libs/printer.jar`

Important vendor-specific details:

- `printer.jar` already contains the generated `PrinterInterface`, `Stub`, and `Proxy` classes. Do not regenerate them.
- Preserve the vendor package spelling `recieptservice.com.recieptservice` exactly. Do not correct it to `recieptservice.com.receiptservice`
