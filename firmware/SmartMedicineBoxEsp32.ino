#include <ArduinoJson.h>
#include <RTClib.h>
#include <WebServer.h>
#include <WiFi.h>
#include <Wire.h>
#include <hd44780.h>
#include <hd44780ioClass/hd44780_I2Cexp.h>
#include <time.h>

#if __has_include("secrets.h")
#include "secrets.h"
#else
#warning "secrets.h is missing; copy secrets.example.h before uploading"
#include "secrets.example.h"
#endif

// Update these pins to match the actual wiring before uploading.
constexpr uint8_t LED_PIN = 26;
constexpr uint8_t BUZZER_PIN = 25;
constexpr uint8_t IR_PIN = 34;
constexpr uint8_t I2C_SDA_PIN = 21;
constexpr uint8_t I2C_SCL_PIN = 22;
constexpr uint8_t MEDICINE_PRESENT_IR_STATE = LOW;
constexpr uint8_t MAX_SCHEDULES = 16;
constexpr unsigned long REMINDER_TIMEOUT_MS = 30UL * 60UL * 1000UL;
constexpr char FIRMWARE_VERSION[] = "1.1.0";

struct MedicineSchedule {
  int id = 0;
  String name;
  String dosage;
  String time;
  int compartment = 1;
  bool active = false;
  int lastTriggeredDay = -1;
};

struct DeviceEvent {
  unsigned long sequence = 0;
  int medicineId = 0;
  String status;
  String confirmationTime;
};

WebServer server(80);
hd44780_I2Cexp lcd;
RTC_DS3231 rtc;
MedicineSchedule schedules[MAX_SCHEDULES];
DeviceEvent latestEvent;
int reminderMedicineId = 0;
unsigned long reminderStartedAt = 0;
bool reminderActive = false;
bool previousIrState = HIGH;
unsigned long lastIrChangeAt = 0;
unsigned long lastLcdUpdateAt = 0;
bool lcdReady = false;
bool rtcReady = false;
String previousLcdLine1;
String previousLcdLine2;

String fitLcdLine(String value) {
  if (value.length() > 16) value.remove(16);
  while (value.length() < 16) value += ' ';
  return value;
}

void showLcd(const String &line1, const String &line2) {
  if (!lcdReady) return;
  const String fittedLine1 = fitLcdLine(line1);
  const String fittedLine2 = fitLcdLine(line2);
  if (fittedLine1 == previousLcdLine1 && fittedLine2 == previousLcdLine2) return;

  lcd.setCursor(0, 0);
  lcd.print(fittedLine1);
  lcd.setCursor(0, 1);
  lcd.print(fittedLine2);
  previousLcdLine1 = fittedLine1;
  previousLcdLine2 = fittedLine2;
}

void updateLcd() {
  if (!lcdReady || millis() - lastLcdUpdateAt < 500) return;
  lastLcdUpdateAt = millis();

  if (reminderActive) {
    const int index = findSchedule(reminderMedicineId);
    if (index >= 0) {
      showLcd("Take " + schedules[index].name,
              schedules[index].dosage + " Box " + String(schedules[index].compartment));
      return;
    }
  }

  const String now = currentTime();
  showLcd("Smart Med Box", now.isEmpty() ? "Syncing time..." : "WiFi  " + now);
}

bool isAuthorized() {
  if (server.header("X-Device-Key") == DEVICE_TOKEN) return true;
  server.send(401, "application/json", "{\"error\":\"unauthorized\"}");
  return false;
}

String currentTime() {
  struct tm timeInfo;
  if (getLocalTime(&timeInfo, 50)) {
    char buffer[6];
    strftime(buffer, sizeof(buffer), "%H:%M", &timeInfo);
    return String(buffer);
  }
  if (!rtcReady) return "";

  const DateTime now = rtc.now();
  char buffer[6];
  snprintf(buffer, sizeof(buffer), "%02d:%02d", now.hour(), now.minute());
  return String(buffer);
}

int currentDay() {
  struct tm timeInfo;
  if (getLocalTime(&timeInfo, 50)) return timeInfo.tm_yday;
  if (!rtcReady) return -1;

  const DateTime now = rtc.now();
  return now.unixtime() / 86400UL;
}

int findSchedule(int medicineId) {
  for (int i = 0; i < MAX_SCHEDULES; i++) {
    if (schedules[i].active && schedules[i].id == medicineId) return i;
  }
  return -1;
}

int findAvailableSchedule() {
  for (int i = 0; i < MAX_SCHEDULES; i++) {
    if (!schedules[i].active) return i;
  }
  return -1;
}

void stopReminder() {
  reminderActive = false;
  reminderMedicineId = 0;
  digitalWrite(LED_PIN, LOW);
  digitalWrite(BUZZER_PIN, LOW);
}

void recordEvent(int medicineId, const String &status) {
  latestEvent.sequence++;
  latestEvent.medicineId = medicineId;
  latestEvent.status = status;
  latestEvent.confirmationTime = currentTime();
}

void handleHealth() {
  if (!isAuthorized()) return;
  JsonDocument response;
  response["deviceId"] = "Smart Medicine Box";
  response["firmwareVersion"] = FIRMWARE_VERSION;
  response["ip"] = WiFi.localIP().toString();
  response["rssi"] = WiFi.RSSI();
  response["reminderActive"] = reminderActive;
  response["time"] = currentTime();
  response["lcdReady"] = lcdReady;
  response["rtcReady"] = rtcReady;
  response["irState"] = digitalRead(IR_PIN);
  response["medicinePresent"] = digitalRead(IR_PIN) == MEDICINE_PRESENT_IR_STATE;
  String body;
  serializeJson(response, body);
  server.send(200, "application/json", body);
}

void handleUpsertSchedule() {
  if (!isAuthorized()) return;
  JsonDocument request;
  if (deserializeJson(request, server.arg("plain"))) {
    server.send(400, "application/json", "{\"error\":\"invalid_json\"}");
    return;
  }

  const int medicineId = request["medicineId"] | 0;
  const char *timeValue = request["time"] | "";
  if (medicineId <= 0 || strlen(timeValue) != 5) {
    server.send(422, "application/json", "{\"error\":\"invalid_schedule\"}");
    return;
  }

  int index = findSchedule(medicineId);
  if (index < 0) index = findAvailableSchedule();
  if (index < 0) {
    server.send(507, "application/json", "{\"error\":\"schedule_capacity_reached\"}");
    return;
  }

  schedules[index].id = medicineId;
  schedules[index].name = request["name"].as<String>();
  schedules[index].dosage = request["dosage"].as<String>();
  schedules[index].time = timeValue;
  schedules[index].compartment = request["compartment"] | 1;
  schedules[index].active = request["active"] | true;

  server.send(200, "application/json", "{\"ok\":true}");
}

void handleDeleteSchedule() {
  if (!isAuthorized()) return;
  const int medicineId = server.arg("id").toInt();
  const int index = findSchedule(medicineId);
  if (index >= 0) schedules[index] = MedicineSchedule();
  if (reminderMedicineId == medicineId) stopReminder();
  server.send(200, "application/json", "{\"ok\":true}");
}

void handleAcknowledge() {
  if (!isAuthorized()) return;
  JsonDocument request;
  if (deserializeJson(request, server.arg("plain"))) {
    server.send(400, "application/json", "{\"error\":\"invalid_json\"}");
    return;
  }
  const int medicineId = request["medicineId"] | 0;
  if (medicineId == reminderMedicineId) stopReminder();
  server.send(200, "application/json", "{\"ok\":true}");
}

void handleLatestEvent() {
  if (!isAuthorized()) return;
  if (latestEvent.sequence == 0) {
    server.send(200, "application/json", "{}");
    return;
  }

  JsonDocument response;
  response["sequence"] = latestEvent.sequence;
  response["medicineId"] = latestEvent.medicineId;
  response["status"] = latestEvent.status;
  response["confirmationTime"] = latestEvent.confirmationTime;
  String body;
  serializeJson(response, body);
  server.send(200, "application/json", body);
}

void checkSchedule() {
  const String now = currentTime();
  const int day = currentDay();
  if (now.isEmpty() || day < 0 || reminderActive) return;

  for (int i = 0; i < MAX_SCHEDULES; i++) {
    if (schedules[i].active && schedules[i].time == now && schedules[i].lastTriggeredDay != day) {
      schedules[i].lastTriggeredDay = day;
      reminderMedicineId = schedules[i].id;
      reminderStartedAt = millis();
      reminderActive = true;
      digitalWrite(LED_PIN, HIGH);
      digitalWrite(BUZZER_PIN, HIGH);
      Serial.printf("Reminder: %s, compartment %d\n", schedules[i].name.c_str(), schedules[i].compartment);
      break;
    }
  }
}

void checkIrSensor() {
  const bool state = digitalRead(IR_PIN);
  if (state != previousIrState && millis() - lastIrChangeAt > 80) {
    previousIrState = state;
    lastIrChangeAt = millis();
    if (reminderActive && state != MEDICINE_PRESENT_IR_STATE) {
      const int confirmedMedicineId = reminderMedicineId;
      stopReminder();
      recordEvent(confirmedMedicineId, "CONFIRMED");
      Serial.printf("Medicine removal confirmed for medicine %d\n", confirmedMedicineId);
    }
  }
}

void setup() {
  Serial.begin(115200);
  pinMode(LED_PIN, OUTPUT);
  pinMode(BUZZER_PIN, OUTPUT);
  pinMode(IR_PIN, INPUT);
  previousIrState = digitalRead(IR_PIN);
  stopReminder();

  Wire.begin(I2C_SDA_PIN, I2C_SCL_PIN);
  rtcReady = rtc.begin();
  Serial.println(rtcReady ? "DS3231 RTC detected." : "DS3231 RTC not detected.");
  const int lcdStatus = lcd.begin(16, 2);
  lcdReady = lcdStatus == 0;
  if (lcdReady) {
    lcd.backlight();
    showLcd("Smart Med Box", "Connecting WiFi");
    Serial.println("LCD detected on SDA 21 / SCL 22.");
  } else {
    Serial.printf("LCD not detected (status %d) on SDA 21 / SCL 22.\n", lcdStatus);
  }

  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.printf("Connecting to %s", WIFI_SSID);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print('.');
  }
  Serial.printf("\nIP: %s\n", WiFi.localIP().toString().c_str());
  Serial.println("Use DEVICE_TOKEN as the app pairing token.");
  showLcd("WiFi connected", WiFi.localIP().toString());

  configTime(UTC_OFFSET_SECONDS, 0, "pool.ntp.org", "time.nist.gov");
  struct tm timeInfo;
  if (getLocalTime(&timeInfo, 10000) && rtcReady) {
    rtc.adjust(DateTime(
        timeInfo.tm_year + 1900,
        timeInfo.tm_mon + 1,
        timeInfo.tm_mday,
        timeInfo.tm_hour,
        timeInfo.tm_min,
        timeInfo.tm_sec));
    Serial.println("RTC synchronized from NTP.");
  }

  const char *headerKeys[] = {"X-Device-Key"};
  server.collectHeaders(headerKeys, 1);
  server.on("/api/health", HTTP_GET, handleHealth);
  server.on("/api/schedule", HTTP_POST, handleUpsertSchedule);
  server.on("/api/schedule", HTTP_DELETE, handleDeleteSchedule);
  server.on("/api/ack", HTTP_POST, handleAcknowledge);
  server.on("/api/event", HTTP_GET, handleLatestEvent);
  server.onNotFound([]() {
    server.send(404, "application/json", "{\"error\":\"not_found\"}");
  });
  server.begin();
}

void loop() {
  server.handleClient();
  checkSchedule();
  checkIrSensor();
  updateLcd();

  if (reminderActive && millis() - reminderStartedAt >= REMINDER_TIMEOUT_MS) {
    const int missedMedicineId = reminderMedicineId;
    stopReminder();
    recordEvent(missedMedicineId, "MISSED");
  }
  delay(5);
}
