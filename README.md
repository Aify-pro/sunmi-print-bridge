# Sunmi Print Bridge

Headless companion APK, shared by several Aify-pro web apps (originally the banking kiosk, now also Seritex). Receives `sunmiprint://` URLs from Chrome, prints on the Sunmi V3H's built-in thermal printer via AIDL, and closes immediately.

## How it works

```
Chrome (Print button tap)
  → window.location.href = "sunmiprint://<type>?data=<base64-JSON>"
  → Android opens PrintActivity (registered for sunmiprint:// scheme)
  → PrintActivity decodes JSON, binds AIDL printer service, prints, finish()
  → User is back in Chrome instantly
```

`<type>` (the URL host) picks the print layout — `receipt` (default) or `label`, see below.

## Kiosk mode (`KioskActivity`)

The same APK also contains a home-screen activity that shows
`https://seritex.vercel.app` in a WebView (no address bar, screen kept on,
Back ignored at the root, network errors retried every 5 s). The bottom
navigation bar is hidden, but the top status bar stays visible and pullable
(quick settings, Wi-Fi toggle) on purpose. `sunmiprint://` links fired by the
page are routed to `PrintActivity`, so the print buttons work inside the
kiosk exactly as they do in Chrome.

Works on the Sunmi V3H and on the Sunmi V2 (Android 7.1.1, `minSdk 24`).
The WebView engine of the V2 ships as Chrome 62, too old for the current
Next.js bundle: update *Android System WebView* on the device first.

A `BootReceiver` starts `KioskActivity` right after `BOOT_COMPLETED`, on top
of whatever launcher is underneath. This is needed on the Sunmi V2: its ROM
ignores the standard Android "default home app" preference — confirmed with
`cmd package set-home-activity`, which does persist in
`dumpsys package`'s Preferred Activities, but is not honored at boot or on a
fresh `HOME` intent, always resolving back to `woyou.launcher` regardless. On
a device that does honor it (e.g. the V3H), pressing **Home** once and picking
this app with **Always** works too, as a normal launcher choice.

`system_server` itself also force-relaunches `woyou.launcher` roughly 15 s
after boot on the V2 — `logcat` shows `PMV2Utils: CUSTOM_LAUNCHER:
com.woyou.launcher` followed by an explicit `START` from the system UID, an
OEM behaviour hardcoded below anything `pm`/`cmd` can reach. `KioskActivity`
fights back with a `Handler` loop (`WATCHDOG_INTERVAL_MS`, 2.5 s) that
re-issues `startActivity` on itself; harmless when already in front
(`singleTask` → `onNewIntent`, no reload), and takes focus back otherwise.

To undo: `adb uninstall com.kiosk.printbridge`.

If the app was just (re)installed, Android withholds `BOOT_COMPLETED` until
it has been launched at least once (the app's "stopped" state) — open it
manually one time (e.g. `adb shell am start -n
com.kiosk.printbridge/.KioskActivity`) before the first reboot you test.

The task is pinned with `startLockTask()`. Without Device Owner enrolment
(which needs a factory reset), Android keeps its standard escape hatch: press
and hold **Back + Recents** together.

## Build (no local tools needed)

### Option 1: GitHub Actions (recommended)
1. Push this repo to GitHub
2. Go to **Actions** → **Build APK** → **Run workflow**
3. Download the APK from the build artifacts
4. Transfer to V3H via USB/ADB and install

### Option 2: Local build (requires JDK 17 + Android SDK)
```bash
cd android/sunmi-print-bridge
./gradlew assembleDebug
# APK at app/build/outputs/apk/debug/app-debug.apk
```

## Install on device
```bash
adb install app-debug.apk
```
Or transfer the APK file to the device and tap to install (enable "Install from unknown sources" first).

## JSON receipt format (`sunmiprint://receipt?data=...`)

```json
{
  "title": "Queue Ticket",
  "brand": "MTN",
  "status": "Submitted",
  "ticketId": "TKD-095",
  "date": "24/02/2026, 10:30:00 AM",
  "message": "Your ticket information has been sent to you by SMS."
}
```

All fields are optional except `title`.

## JSON label format (`sunmiprint://label?data=...`)

A centered label with a scannable QR code — used by Seritex to print a waste-bag
tag directly on the terminal's printer, with no PDF and no download.

```json
{
  "header": "SERITEX · SAC DE DÉCHETS",
  "qrData": "https://app.seritex.example/dechets/SAC-2026-00042",
  "code": "SAC-2026-00042",
  "infoLine": "Créé le 18 sept. 2026"
}
```

All fields are optional, but a label with no `qrData` just prints text.

## Commands format (`sunmiprint://commands?data=...`)

The caller owns the whole layout; the bridge only executes the list, in order.
The paper is 58 mm wide with 384 printable dots.

```json
{"ops": [
  {"op": "align", "v": 1},
  {"op": "text", "v": "SERITEX", "size": 26},
  {"op": "feed", "n": 1},
  {"op": "qr", "v": "https://...", "module": 11, "level": 1},
  {"op": "feed", "n": 4}
]}
```

`align` v: 0 left, 1 center, 2 right. `text` size: font size (default 24).
`qr` module: dots per module 1-16 (default 6), level: 0=L 1=M 2=Q 3=H.
`feed` n: lines of paper (max 20).
