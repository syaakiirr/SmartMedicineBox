#pragma once

// Copy this file to secrets.h and fill in values before uploading.
#define WIFI_SSID "YOUR_WIFI_NAME"
#define WIFI_PASSWORD "YOUR_WIFI_PASSWORD"
#define DEVICE_TOKEN "CHANGE_TO_A_LONG_RANDOM_TOKEN"

// The box always exposes this hotspot for direct phone-to-device sync.
// AP_PASSWORD must contain between 8 and 63 characters.
#define AP_SSID "SmartMedBox"
#define AP_PASSWORD "CHANGE_THIS_HOTSPOT_PASSWORD"

// Malaysia is UTC+8. Change this when the device is used elsewhere.
#define UTC_OFFSET_SECONDS 28800
