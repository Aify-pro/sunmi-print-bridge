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
  "qrModuleSize": 9,
  "code": "SAC-2026-00042",
  "infoLine": "Créé le 18 sept. 2026"
}
```

All fields are optional, but a label with no `qrData` just prints text.
`qrModuleSize` (dots per QR module, 1-16, default 6) lets the caller fill the
384-dot printable width: `floor(376 / modulesInTheQr)`.
