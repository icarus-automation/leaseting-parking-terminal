# Test locally on an H10S

Assumes the frontend and `leaseting-api` are already running. The API must use port `8000`.

## One-time setup

Add this to the terminal project's `local.properties`:

```properties
leaseting.api.baseUrl=http://localhost:8000/api/v1/
```

Rebuild and reinstall the app after changing `local.properties`.

## Each USB session

Connect the H10S with USB debugging enabled, then run:

```bash
adb devices
adb reverse tcp:8000 tcp:8000
```

In Android Studio, select the H10S and run the `app` configuration. Sign in with a `parking_attendant` account.

Run `adb reverse tcp:8000 tcp:8000` again whenever the USB connection restarts.

## If the app cannot reach the server

Check the forwarding rule:

```bash
adb reverse --list
```

If port `8000` is missing, run the reverse command again. If it is listed, confirm the API listens on `localhost:8000` on your computer. Rebuild and reinstall if `local.properties` changed.
