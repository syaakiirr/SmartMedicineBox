# ESP32 Hotspot Firmware

This firmware exposes the REST API used by the Android app. The ESP32 always starts its own hotspot for direct sync and can also join home Wi-Fi to update the RTC from NTP.

## Requirements

- ESP32 board support in Arduino IDE
- ArduinoJson 7.x from Library Manager
- ESP32, LED, buzzer, and IR sensor wiring that matches the pin constants in `SmartMedicineBoxEsp32.ino`

## Setup

1. Copy `secrets.example.h` to `secrets.h`.
2. Set a long random `DEVICE_TOKEN` and change `AP_PASSWORD`. Set `WIFI_SSID` and `WIFI_PASSWORD` when home Wi-Fi should be used for NTP; otherwise leave the placeholders unchanged.
3. Confirm `LED_PIN`, `BUZZER_PIN`, and `IR_PIN` match the physical wiring.
4. Open the `firmware` folder through `firmware.ino` in Arduino IDE and upload it to the ESP32.
5. Open Serial Monitor at 115200 baud and confirm that the hotspot has started.
6. Connect the phone to `AP_SSID` using `AP_PASSWORD`. Android may report that this network has no internet; stay connected.
7. On the Android dashboard, tap the connection badge and enter `192.168.4.1` and the same `DEVICE_TOKEN`.

The hotspot gateway remains `192.168.4.1`, so no router or DHCP reservation is required. When home Wi-Fi is configured, the REST API also remains reachable through the station IP printed in Serial Monitor.

## API contract

- `GET /api/health`: authenticated device heartbeat.
- `POST /api/schedule`: creates or updates one medicine schedule.
- `DELETE /api/schedule?id=<medicineId>`: removes a schedule.
- `POST /api/ack`: stops the active reminder after an app action.
- `GET /api/event`: returns the latest IR-confirmed or missed event.

Every request requires `X-Device-Key` with the token from `secrets.h`.

Schedules are currently held in ESP32 memory and are re-sent by the app whenever it reconnects. A future production pass can persist them with ESP32 Preferences/NVS.
