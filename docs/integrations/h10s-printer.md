# H10S `printer.jar` integration reference

## 1. Scope and source of truth

This guide is the canonical reference for integrating:

```text
app/libs/printer.jar
```

Expected SHA-256:

```text
4bc2deaf3ec42166f3d8b525f72e0a61fa5a1f484486a3a43f2bfba480f8efc6
```

The compiled JAR is the source of truth for available classes and methods.

Do not:

- Use the demo AAR.
- Use `SrPrinter.getInstance(Context)`. That method is absent from this JAR.
- Add raw AIDL files. The JAR already contains the generated Binder classes.
- Correct the vendor spelling `recieptservice`.
- Invent status, cleanup, callback, or error interfaces absent from the JAR.

## 2. JAR dependency setup

Place the exact manufacturer JAR at:

```text
app/libs/printer.jar
```

Add this module dependency:

```kotlin
dependencies {
    implementation(files("libs/printer.jar"))
}
```

No Maven repository or additional printer dependency is documented.

## 3. AIDL source files

The application requires zero `.aidl` source files.

`printer.jar` already contains:

```text
recieptservice.com.recieptservice.PrinterInterface
recieptservice.com.recieptservice.PrinterInterface.Stub
recieptservice.com.recieptservice.PrinterInterface.Stub.Proxy
```

Use:

```kotlin
import recieptservice.com.recieptservice.PrinterInterface
```

Do not create `app/src/main/aidl` for this integration.

The selected JAR does not contain the newer scanner or PSAM methods found in other SDK artifacts.

## 4. Manifest permissions

No manifest permission is proven to be required for printing.

An older manufacturer demo declared:

```xml
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
```

No inspected demo code, JAR interface, or manufacturer document established that permission as a printing requirement. Do not add it unless H10S device testing or updated manufacturer documentation proves it is required.

## 5. Printer service component

The JAR binds this explicit Android service:

| Field | Exact value |
| --- | --- |
| Service package | `recieptservice.com.recieptservice` |
| Service class | `recieptservice.com.recieptservice.service.PrinterService` |
| Binding flag | `Service.BIND_AUTO_CREATE` |

The service must already be installed on the H10S device.

## 6. Binding Intent action

There is no Intent action string.

The JAR internally creates an empty `Intent` and sets the explicit component:

```kotlin
Intent().setClassName(
    "recieptservice.com.recieptservice",
    "recieptservice.com.recieptservice.service.PrinterService",
)
```

`android.intent.action.MAIN` is only an application launcher action. It is unrelated to printer binding.

## 7. Service initialization

The public `SrPrinter` interface contains only:

```java
public static void bindPrinter(
    android.content.Context context,
    android.content.ServiceConnection connection
);
```

Required imports:

```kotlin
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import com.sr.SrPrinter
import recieptservice.com.recieptservice.PrinterInterface
```

Minimal Kotlin translation:

```kotlin
private var printerInterface: PrinterInterface? = null

private val printerConnection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName, binder: IBinder) {
        printerInterface = PrinterInterface.Stub.asInterface(binder)
    }

    override fun onServiceDisconnected(name: ComponentName) {
        printerInterface = null
    }
}

fun bindPrinter(context: Context) {
    SrPrinter.bindPrinter(context, printerConnection)
}
```

`SrPrinter.bindPrinter` internally:

1. Creates the explicit service Intent.
2. Calls `Context.startService(intent)`.
3. Calls `Context.bindService(intent, connection, Service.BIND_AUTO_CREATE)`.
4. Returns `void`.

The JAR does not expose the result of `bindService`. Treat `onServiceConnected` as the only documented point at which `PrinterInterface` becomes available.

Call printing methods only after `onServiceConnected`.

## 8. Print and control methods

The selected JAR's `PrinterInterface` exposes these exact methods.

### Output methods

```text
void printEpson(byte[] data)
void printText(String text)
void printBitmap(Bitmap pic)
void printBarCode(String data, int symbology, int height, int width)
void printQRCode(String data, int modulesize, int errorlevel)
void nextLine(int line)
void printTableText(String[] text, int[] weight, int[] alignment)
void printPDF417Code(String data, int modulesize, int errorlevel)
void print128BarCode(String data, int type, int height, int width)
```

### Formatting and work-control methods

```text
void setAlignment(int alignment)
void setTextSize(float textSize)
void setTextBold(boolean bold)
void beginWork()
void endWork()
void setDark(int value)
void setLineHeight(float lineHeight)
void setTextDoubleWidth(boolean enable)
void setTextDoubleHeight(boolean enable)
void setCode(String code)
```

### Informational method

```text
String getServiceVersion()
```

All methods above are Binder calls and declare `RemoteException` in their generated Java interface.

### Documented parameter meanings

`printBarCode`:

| `symbology` | Type |
| ---: | --- |
| `0` | UPC-A |
| `1` | UPC-E |
| `2` | JAN13 / EAN13 |
| `3` | JAN8 / EAN8 |
| `4` | CODE39 |
| `5` | ITF |
| `6` | CODABAR |
| `7` | CODE93 |
| `8` | CODE128 |

Documented barcode ranges:

```text
height: 1 through 255, documented default 162
width: 2 through 6, documented default 2
```

`printQRCode`:

```text
modulesize: 1 through 16
errorlevel: 0 through 3
```

`setAlignment`:

```text
0: left
1: center
2: right
```

No verified parameter semantics are available here for `printPDF417Code`, `print128BarCode`, `setCode`, darkness, or line height. Do not invent ranges or meanings.

## 8a. Receipt line layout

`printTableText` is the only two-column primitive in the JAR, and it is not
usable for label/value rows on 58 mm paper.

Observed behaviour with `weight = {1, 1}`:

- Each column gets half the width, about 16 characters at `setTextSize(24f)`.
- A value wider than its column is broken wherever the overflow falls, inside a
  word or inside a number.

Two field receipts from a live gate:

```text
Time out   09 Sep 2026, 7:
46 PM
Attendant   Parking Attenda
nt
```

No pair of weights fixes this. Weights are relative and the service rounds them
itself, and any column narrow enough to leave room for a long value is too
narrow for the next one.

The application lays rows out itself instead, in
`data/printing/ReceiptLayout.kt`, and prints the finished strings with
`printText`:

- One line per row whenever `label + value` fits the width, with the value
  padded out to the right margin.
- Otherwise the label keeps its line and the value wraps beneath it at word
  boundaries, right aligned. A single token wider than the paper is chopped
  rather than dropped.

```text
Time out    09 Sep 2026, 7:46 PM
Attendant      Parking Attendant
```

### The font is single width for ASCII and double width for everything else

Laying rows out by counting characters is not enough, and the second field
receipt is what proved it. The three money lines came back with their last digit
orphaned:

```text
AMOUNT DUE              ₱150.0
0
```

Every one of those lines was 32 characters, the same as `Time out` and
`Attendant`, which printed fine. The only difference is the peso sign. `₱` is
outside ASCII, and the printer's bitmap font draws it in a double-width cell, so
a 32-character money line is 33 cells and the roll drops the last one.

`ReceiptLayout.cells()` is the measurement everything uses:

```text
"₱150.00"   7 characters, 8 cells
```

A money row is now 31 characters and 32 cells. `ReceiptLayoutTest` asserts the
cell width of every line it produces, so a row can no longer be a character
short of wrapping without a test saying so.

This is the usual arrangement on these printers (ASCII 12x24, everything else
24x24) and the JAR exposes no way to ask about it. It was derived from the
observed break, not assumed.

### The one assumption left

| Assumption | Where it lives | If it turns out wrong |
| --- | --- | --- |
| 58 mm holds 32 cells at `TEXT_SIZE_BODY` | `ReceiptLayout.COLUMNS` | Change that one constant. The separator rule and every row follow it. |

It predates this layout: the separator rule has always printed `"-".repeat(32)`
through `printText` and relied on the same cell.

`ReceiptLayout` is pure Kotlin with no Android dependency, and
`ReceiptLayoutTest` pins the field cases above plus the whole exit receipt as a
golden block. Change the layout there and read the diff, rather than feeding
paper through a handheld to find out.

### Month names

Unrelated to the JAR, but it reached the same paper. `DateTimeFormatter` asked
`Locale.ROOT` for `MMM`, and Android resolves root through ICU, which answers
with CLDR's placeholder rather than an English abbreviation:

```text
09 M09 2026
```

`ui/format/ParkingFormat.kt` names `Locale.US` for the date pattern. Times and
money stay independent of whatever locale the handheld is set to.

## 9. Printer status

The selected JAR provides no supported printer readiness, paper, hardware-error, or connection-status query.

`getServiceVersion()` returns the printer service version. It is not printer status.

The selected JAR's `PrinterInterface` does not contain:

```text
getScannerStatus()
checkPSAMCard()
activatePSAMCard()
deactivatePSAMCard()
transmitPSAMCard()
```

Do not infer support for those methods from another H10S SDK version.

Application connection state can only be derived from the `ServiceConnection` callbacks documented above.

## 10. Error handling

Verified behavior:

- `SrPrinter.bindPrinter` returns `void`.
- `SrPrinter.bindPrinter` provides no explicit bind-success or bind-failure result.
- `PrinterInterface` calls declare `RemoteException`.
- Print methods return `void`.
- No print-success callback is provided.
- No printer error-code table is provided.

The application must define how its printer module translates:

- A missing connection.
- `RemoteException`.
- Service disconnection.
- A failed or interrupted receipt.

Those policies are application decisions. This guide does not prescribe retry, duplicate prevention, or user messaging because the manufacturer interface does not define them.

## 11. Cleanup and service unbinding

The selected printer integration surface, `SrPrinter` and `PrinterInterface`:

- `SrPrinter` exposes `bindPrinter`.
- Neither interface exposes `unbind`, `close`, or shutdown.
- The manufacturer provides no documented cleanup sequence for this printer binding.

The application owns the supplied `ServiceConnection`. Binding ownership and any matching Android `unbindService(connection)` call must be decided by the application architecture. Do not present a cleanup sequence as a manufacturer requirement.

## 12. ProGuard and R8

No printer-specific ProGuard or R8 rules were supplied with the verified SDK artifacts.

Do not add speculative keep rules.

If minification is enabled later, verify a release build and printing on a physical H10S device before adding any rules.

## 13. Verification commands

Inspect the selected binary instead of relying on another SDK version:

```text
javap -classpath app/libs/printer.jar -p com.sr.SrPrinter
javap -classpath app/libs/printer.jar -p recieptservice.com.recieptservice.PrinterInterface
javap -classpath app/libs/printer.jar -c com.sr.SrPrinter
```

Expected `SrPrinter` result:

```text
public static void bindPrinter(
    android.content.Context,
    android.content.ServiceConnection
);
```

If the JAR hash or compiled signatures differ, stop and update this reference from the actual binary before implementing.
