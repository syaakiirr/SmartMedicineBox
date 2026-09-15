# ESP32 Wi-Fi Firmware

This firmware exposes the REST API used by the Android app and keeps Wi-Fi credentials outside source control.

## Requirements

- ESP32 board support in Arduino IDE
- ArduinoJson 7.x from Library Manager
- ESP32, LED, buzzer, and IR sensor wiring that matches the pin constants in `SmartMedicineBoxEsp32.ino`

## Setup

1. Copy `secrets.example.h` to `secrets.h`.
2. Set `WIFI_SSID`, `WIFI_PASSWORD`, and a long random `DEVICE_TOKEN`.
3. Confirm `LED_PIN`, `BUZZER_PIN`, and `IR_PIN` match the physical wiring.
4. Open the `firmware` folder through `firmware.ino` in Arduino IDE and upload it to the ESP32.
5. Open Serial Monitor at 115200 baud and note the printed IP address.
6. On the Android dashboard, tap the connection badge and enter that IP and the same `DEVICE_TOKEN`.

The phone and ESP32 must use the same Wi-Fi network. Give the ESP32 a DHCP reservation in the router so its IP remains stable.

## API contract

- `GET /api/health`: authenticated device heartbeat.
- `POST /api/schedule`: creates or updates one medicine schedule.
- `DELETE /api/schedule?id=<medicineId>`: removes a schedule.
- `POST /api/ack`: stops the active reminder after an app action.
- `GET /api/event`: returns the latest IR-confirmed or missed event.

Every request requires `X-Device-Key` with the token from `secrets.h`.

Schedules are currently held in ESP32 memory and are re-sent by the app whenever it reconnects. A future production pass can persist them with ESP32 Preferences/NVS.
