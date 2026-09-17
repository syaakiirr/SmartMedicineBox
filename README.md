# Smart Medicine Box

Android app + ESP32 firmware that remind patients to take scheduled medication and let caregivers see whether the medicine box was actually accessed. Reminders fire on both sides at once: the phone raises an exact-alarm notification while the ESP32 lights its LED, sounds its buzzer, and shows the reminder on its LCD.

> Accuracy note: the IR sensor proves **box access**, not ingestion. The UI says “Box accessed” deliberately.

## Features

- **Patient view** — today's schedule, next-medicine panel, exact-time picker (any minute, 24h), record-access / mark-missed actions with instant notification dismissal.
- **Caregiver view** — separate navigation and overview prioritising due, missed, and recent box activity; zero counts stay quiet instead of red.
- **Local reminders** — exact daily alarms with alarm sound + vibration, rescheduled after boot, package update, and time/timezone changes (Android 13+ notification permission requested).
- **ESP32 sync** — automatic REST sync over the box's own password-protected Wi-Fi hotspot: heartbeat, schedule push/delete, acknowledgment, and IR event import. Home-LAN API access still requires `X-Device-Key`. Schedules are re-sent after every reconnect (ESP32 holds them in RAM).
- **AI medicine assistant** — optional text chat for general medicine information and symptom guidance through the Google Gemini API. It includes medical safety guardrails and is not a diagnosis service.
- **Hardware** — 16×2 I2C LCD (auto-detected), DS3231 RTC (NTP-synced, offline fallback), LED + active buzzer on reminder, IR sensor confirming box access and silencing the reminder.

## Hardware

| Part | Pin |
|---|---|
| LED | GPIO26 |
| Buzzer TMB-12A12 (active, via series resistor) | GPIO25 |
| IR sensor OUT (`LOW` = present, `HIGH` = removed) | GPIO34 |
| LCD 16×2 I2C + RTC | SDA GPIO21, SCL GPIO22 |

For full buzzer volume the TMB-12A12 wants a transistor driver and its rated supply (see firmware notes); the shipped firmware drives it with `HIGH`/`LOW` because it is an active buzzer, not `tone()`.

## Getting started

### Firmware (`firmware/`)

1. Install ESP32 board support + `ArduinoJson 7.x` + `hd44780` LCD library.
2. Copy `firmware/secrets.example.h` to `firmware/secrets.h` and set the home Wi-Fi values, a long random `DEVICE_TOKEN`, and a private `AP_PASSWORD`. **Never commit `secrets.h`.** Home Wi-Fi is optional when the RTC already has the correct time.
3. Flash `SmartMedicineBoxEsp32.ino` (FQBN `esp32:esp32:esp32`), open Serial Monitor at 115200 baud, note the IP.
4. Full pinout and API details: [`firmware/README.md`](firmware/README.md).

### Android app (`app/`)

Requires Android Studio with a JDK; `minSdk 24`.

```bat
.\gradlew.bat testDebugUnitTest assembleDebug
```

Install `app/build/outputs/apk/debug/app-debug.apk`, create an account (Patient or Caregiver role changes the navigation), and connect the phone to the `SmartMedBox` Wi-Fi hotspot. The app automatically finds the box at `192.168.4.1` and syncs without a separate linking step. Android may warn that this Wi-Fi has no internet, which is expected.

### AI assistant

The AI tab is available to both Patient and Caregiver roles without requiring users to enter an API key. Release builds use app-managed Gemini access and Google's `gemini-3.8-flash` model. For local builds, set `GEMINI_API_KEY` in the untracked `local.properties` file or as an environment variable. Because credentials embedded in a mobile APK can be extracted, production keys must be restricted to the Gemini API and the Android app in Google Cloud. Keep mobile data enabled while connected to the local-only `SmartMedBox` hotspot so AI requests can use cellular internet.

Before first use, the app discloses that questions and recent chat context are sent to Google Gemini. Google handles prompts and responses according to the Gemini API terms and the data policy for the API plan; free and paid plans may differ. Users should avoid submitting identifying or sensitive information.

AI responses provide general information only. They must not be treated as a diagnosis, prescription, personalized dosage instruction, or replacement for a doctor or pharmacist. Call Malaysia emergency number 999 for urgent symptoms.

## API contract

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/api/health` | Heartbeat + diagnostics (`lcdReady`, `rtcReady`, `irState`, `medicinePresent`) |
| `POST` | `/api/schedule` | Create/update one medicine schedule |
| `DELETE` | `/api/schedule?id=<medicineId>` | Remove a schedule |
| `POST` | `/api/ack` | Stop the active reminder after an app action |
| `GET` | `/api/event` | Latest IR-confirmed/missed event |

## Project layout

```text
app/        Android (Jetpack Compose, Material 3, Room)
firmware/   ESP32 Arduino sketch + pairing template
PRODUCT.md  Product context
DESIGN.md   Design system
```

## Status

Working prototype: real Wi-Fi pairing, LCD/RTC/LED/buzzer/IR verified on hardware, local + device reminders confirmed end-to-end. Release APKs are unsigned (no `signingConfig`/keystore yet) and build outputs are excluded from version control.
