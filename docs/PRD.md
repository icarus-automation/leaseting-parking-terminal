# Leaseting Parking Terminal PRD

## Product Summary 

Leaseting Parking Terminal is a native Android application for guards operating the Chugasco parking system.

The application runs on the H10S handheld device. It records parking transactions, prints 58 mm receipts using the official H10S printer SDK, works during internet outages, and synchronizes transactions with the existing NestJS backend when connectivity returns.

Parking administration and reports remain in the existing Leaseting web application.

## Problem 

Guards need a dedicated handheld application that can:

- Record parking transactions quickly
- Print required receipts immediately
- Continue operating when internet connectivity is unavailable
- Prevent lost or duplicated transactions
- Synchronize transactions for centralized reports
- Show whether transactions are pending, synced, or require review

## Product Goals 

- Provide a native Android parking terminal for the H10S.
- Print receipts through the official H10S printer SDK.
- Save individual parking transactions locally.
- Continue recording transactions while offline.
- Synchronize pending transactions with leaseting-api.
- Prevent duplicate transactions during synchronization retries.
- Keep centralized reports in leaseting-client.
- Support daily transaction reporting after synchronization.
- Support explicit guard shifts.

## Users and Roles

Guard: The guard uses the H10S application to perform approved parking transactions and print receipts.

Administrator or Manager: The administrator or manager uses leaseting-client

## Backend Integration

The terminal communicates directly with the existing `leaseting-api`. it must not create a separate backend.

### Production API

- Base URL: `https://api.leaseting.com/api/v1/`
- All network requests must use versioned endpoints under `/api/v1`.
