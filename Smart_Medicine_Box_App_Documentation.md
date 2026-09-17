# Smart Medicine Box Application Documentation

## 1. Project Title
**Smart Medicine Box with Mobile Application and Arduino-Based IoT Device**

## 2. Project Overview
The Smart Medicine Box is an IoT-based healthcare project designed to help users manage their medication safely and take medicine according to the correct schedule.

The system consists of a **mobile application**, an **Arduino-programmed ESP32**, and hardware components such as an LCD display, buzzer, LED, RTC module, and IR sensor.

The mobile application is used to manage medication schedules and provide information or notifications to the user and caregiver. The ESP32 acts as the main controller for the physical medicine box. It is programmed using the **Arduino IDE** and connects to the application through an Internet-based communication service.

The main focus of the system is medication safety, caregiver notification, and ensuring that medicine is taken on time.

---

## 3. Project Objectives

The project has three main objectives:

### Objective 1: Improve Medicine Safety
To improve medicine safety by providing an organized medication reminder and monitoring system that helps reduce forgotten or missed medication.

### Objective 2: Notify the Caregiver Through the Application
To develop an application that can notify the caregiver about important medication events, especially when medicine is due, taken, or not taken within the expected time.

### Objective 3: Ensure Medicine Is Taken on Time
To ensure that the user takes medicine according to the scheduled time by providing reminders through the physical medicine box and mobile application.

---

## 4. Proposed System Architecture

```text
+-------------------------+
|     Mobile App          |
|  Medication Management  |
|  Caregiver Monitoring   |
+------------+------------+
             |
             | Internet
             v
+-------------------------+
| Cloud / Database / API  |
| Communication Layer     |
+------------+------------+
             |
             | Wi-Fi
             v
+-------------------------+
|         ESP32           |
| Programmed using        |
|      Arduino IDE        |
+------------+------------+
             |
     +-------+-------+-----------+----------+
     |               |           |          |
     v               v           v          v
+---------+      +--------+   +------+   +--------+
|   LCD   |      | Buzzer |   | LED  |   |   IR   |
| Display |      |        |   |      |   | Sensor |
+---------+      +--------+   +------+   +--------+
```

### Important Note
**Arduino IDE does not directly act as the communication bridge between the mobile app and the device.**

Arduino IDE is used to **write and upload the firmware to the ESP32**. After the program is uploaded, the ESP32 operates independently and communicates with the application using Wi-Fi and the selected backend/API.

```text
Arduino IDE
     |
     | Upload firmware
     v
   ESP32
     |
     | Wi-Fi / Internet
     v
Backend / Database
     ^
     |
     v
Mobile Application
```

---

## 5. System Components

### 5.1 Mobile Application
The mobile application is the main interface for the user and caregiver.

The application should provide:
- Medication schedule management
- Add medicine
- Edit medicine
- Delete medicine
- Medication time setting
- Today's medication schedule
- Medication status
- Medication history
- Caregiver notifications
- Device connection/status information

### 5.2 ESP32
The ESP32 is the main microcontroller of the Smart Medicine Box.

It is programmed through **Arduino IDE**.

Responsibilities:
- Connect to Wi-Fi
- Obtain medication schedule information
- Monitor medication time
- Activate buzzer
- Activate LED
- Display information on LCD
- Read IR sensor
- Update medication status
- Communicate with the backend/mobile application

### 5.3 Arduino IDE
Arduino IDE is used for developing and uploading the ESP32 firmware.

Example responsibilities of the Arduino program:

```cpp
// Conceptual responsibilities

connectToWiFi();
connectToBackend();
getMedicationSchedule();
checkMedicationTime();
activateBuzzer();
updateLCD();
readIRSensor();
updateMedicationStatus();
```

The final implementation may use different functions depending on the selected backend and libraries.

---

## 6. Hardware Components

Proposed hardware:

| Component | Function |
|---|---|
| ESP32 | Main controller and Wi-Fi communication |
| LCD I2C | Displays medication reminders and system status |
| Buzzer | Produces an audible medication reminder |
| LED | Provides a visual medication reminder |
| IR Sensor | Detects activity at the medicine compartment |
| RTC Module | Maintains accurate local time for medication reminders |
| Medicine Box | Stores the medicine |
| Power Supply | Supplies power to the system |

---

## 7. Application Modules

### 7.1 User / Caregiver Module
The application should support the patient/user and caregiver workflow.

**User**
- Views medication schedule
- Receives medication reminders
- Uses the physical medicine box

**Caregiver**
- Monitors medication status
- Receives important medication notifications
- Checks medication history
- Can manage medication schedules if permitted

### 7.2 Medication Management Module
The application should allow medication information to be created and managed.

Example data:

```text
Medicine Name: Paracetamol
Dosage: 1 Tablet
Time: 08:00 AM
Status: Pending
```

Functions:
- Add medication
- Edit medication
- Delete medication
- Set medication time
- Set dosage

### 7.3 Medication Reminder Module
When the scheduled medication time is reached:

```text
Scheduled Time
      |
      v
ESP32 checks schedule
      |
      +------------------+
      |                  |
      v                  v
 Buzzer + LED        LCD Display
      |                  |
      +---------+--------+
                |
                v
         User is reminded
```

The application can also display/send a medication reminder.

### 7.4 Medicine Detection Module
The IR sensor is used to detect the configured activity at the medicine compartment.

```text
User interacts with medicine
            |
            v
        IR Sensor
            |
            v
          ESP32
            |
            v
     Detection Logic
            |
            v
   Update Medication Status
```

**Important:** IR detection alone may indicate that the compartment or medicine was interacted with; it does not medically prove that the user swallowed the medicine. The application should describe the event accurately based on what the hardware can actually detect.

### 7.5 Caregiver Notification Module
The caregiver can receive notifications based on medication status.

Possible notifications:

```text
Medicine Due
"Medicine 1 is due at 8:00 AM."
```

```text
Medicine Activity Confirmed
"Medicine 1 was accessed at 8:03 AM."
```

```text
Medicine Missed
"Medicine 1 has not been confirmed. Please check the user."
```

This module directly supports **Objective 2: Notify the caregiver through the application.**

---

## 8. Medication Status

The system can use four main statuses:

| Status | Meaning |
|---|---|
| Pending | Medication is scheduled but the time has not arrived |
| Due | Medication time has arrived |
| Taken / Confirmed | Expected medicine-box activity has been detected or confirmed |
| Missed | No confirmation was received within the configured time period |

For safety, the final app terminology should match the actual detection capability. If the sensor only detects box access, **Confirmed/Accessed** is more accurate than claiming ingestion was proven.

---

## 9. Main Application Screens

### 9.1 Login Screen
Purpose:
- User authentication
- Caregiver authentication

### 9.2 Dashboard
The dashboard should show the most important information.

Example:

```text
SMART MEDICINE BOX

Device: Online

Next Medicine
-----------------------
Paracetamol
1 Tablet
08:00 AM
Status: Pending

Today's Medication
-----------------------
08:00 AM  Medicine 1
02:00 PM  Medicine 2
08:00 PM  Medicine 3
```

### 9.3 Add Medicine Screen
Fields:

```text
Medicine Name
Dosage
Medication Time
Reminder Settings

[ SAVE MEDICINE ]
```

### 9.4 Medication Schedule Screen
Displays all scheduled medicine.

```text
08:00 AM
Medicine 1
Pending

02:00 PM
Medicine 2
Pending

08:00 PM
Medicine 3
Pending
```

### 9.5 Medication History Screen
Example:

```text
DATE        MEDICINE       TIME       STATUS
14/09/2026  Medicine 1     08:03 AM   Confirmed
14/09/2026  Medicine 2     02:00 PM   Missed
14/09/2026  Medicine 3     08:05 PM   Confirmed
```

### 9.6 Caregiver Screen
The caregiver dashboard can display:

```text
Patient Medication Status

Medicine 1
08:00 AM
Confirmed

Medicine 2
02:00 PM
Missed

Medicine 3
08:00 PM
Pending
```

---

## 10. Complete System Workflow

### Step 1 — Set Medication
The user or caregiver adds medicine through the application.

```text
Open App
   |
   v
Add Medicine
   |
   v
Enter Medicine Information
   |
   v
Set Medication Time
   |
   v
Save
```

### Step 2 — Synchronize Schedule

```text
Mobile App
    |
    v
Backend / Database
    |
    v
ESP32
```

The ESP32 obtains the information required to perform the medication reminder.

### Step 3 — Medication Time

Example:

```text
Current Time = 08:00 AM
Scheduled Time = 08:00 AM

        MATCH
          |
          v
+---------------------+
| Medication Reminder |
+---------------------+
      |       |
      v       v
   Buzzer    LED
      |
      v
LCD: "Take Medicine 1"
```

### Step 4 — App Notification
The application/caregiver notification system generates the appropriate reminder.

```text
Medication Reminder

Medicine 1 is due now.
Scheduled Time: 08:00 AM
```

### Step 5 — Detect Medicine Box Activity

```text
User accesses medicine
       |
       v
   IR Sensor
       |
       v
     ESP32
       |
       v
Status Updated
```

### Step 6 — Caregiver Update

```text
ESP32
   |
   v
Backend
   |
   v
Mobile App
   |
   v
Caregiver sees updated status
```

### Step 7 — Missed Medication
If there is no confirmation after a configured period:

```text
Medication Due
      |
      v
Wait for Confirmation
      |
      v
No Confirmation
      |
      v
Status = MISSED
      |
      v
Notify Caregiver
```

---

## 11. Proposed Data Structure

### User

```text
userId
name
email
role
```

### Medicine

```text
medicineId
userId
medicineName
dosage
scheduledTime
compartment (legacy compatibility field, fixed to 1)
active
```

### Medication Record

```text
recordId
medicineId
scheduledDate
scheduledTime
status
confirmationTime
```

### Device

```text
deviceId
userId
onlineStatus
lastConnection
```

---

## 12. Functional Requirements

The system should be able to:

1. Allow medication information to be added.
2. Allow medication information to be edited.
3. Allow medication information to be deleted.
4. Allow medication schedules to be configured.
5. Store medication schedule information.
6. Connect the ESP32 to Wi-Fi.
7. Allow the ESP32 to obtain relevant medication information.
8. Activate the buzzer at medication time.
9. Activate the LED at medication time.
10. Display medication information on the LCD.
11. Detect medicine-box activity using the IR sensor.
12. Update medication status.
13. Display medication status in the application.
14. Store medication history.
15. Notify the caregiver of important medication events.
16. Identify missed medication according to the configured reminder rule.

---

## 13. Non-Functional Requirements

### Usability
The application should have a simple interface suitable for everyday medication management.

### Reliability
Medication schedules and status should remain consistent between the application and medicine box.

### Performance
Medication information and status updates should be transmitted within a reasonable period when an Internet connection is available.

### Security
User, caregiver, and medication information should only be accessible to authorized users.

### Availability
The physical reminder should be designed to continue functioning as reliably as possible even when temporary network issues occur. Critical reminder logic should not depend entirely on a live phone connection.

---

## 14. Safety Requirements

Because the system is related to medication, safety should be considered throughout development.

- The system should not provide medical diagnosis.
- The system should not modify medication dosage automatically.
- Medicine names and schedules should be entered or confirmed by an authorized user.
- The device should clearly identify which medicine is due.
- A network failure should be visible to the user/caregiver.
- Sensor activity should not be presented as proof of ingestion unless the hardware can actually verify ingestion.
- Missed medication should trigger an appropriate caregiver alert.
- The application should not instruct a user to double a dose after a missed medication.

---

## 15. Minimum Viable Product (MVP)

The first working version should focus on the three project objectives.

### MVP Features

```text
1. Add medicine
        |
2. Set medication time
        |
3. Send schedule to ESP32
        |
4. ESP32 checks time
        |
5. Buzzer + LED + LCD reminder
        |
6. App notification
        |
7. IR sensor detects activity
        |
8. ESP32 updates status
        |
9. App displays status
        |
10. Caregiver receives alert when required
```

Features such as advanced analytics, complex user profiles, themes, and other optional functions should be developed only after this workflow is stable.

---

## 16. Development Plan

### Phase 1 — Mobile Application
Build:
- Project structure
- Login
- Dashboard
- Add medicine
- Medication schedule
- History
- Caregiver interface

### Phase 2 — Arduino / ESP32
Using Arduino IDE:
- ESP32 Wi-Fi
- LCD
- RTC
- Buzzer
- LED
- IR sensor
- Reminder logic

### Phase 3 — Backend Integration
Connect:

```text
App <--> Backend <--> ESP32
```

Implement:
- Medication data
- Schedule synchronization
- Status synchronization
- Caregiver notification

### Phase 4 — Integration Testing
Test the complete flow:

```text
Set Medicine
     |
     v
ESP32 Receives Schedule
     |
     v
Reminder
     |
     v
Medicine Activity
     |
     v
Status Update
     |
     v
Caregiver Notification
```

### Phase 5 — Final Testing
Test:
- Multiple medicines
- Correct medication time
- Buzzer
- LED
- LCD
- IR sensor
- Internet disconnection
- Device reconnection
- App notification
- Caregiver notification
- Missed medication
- Medication history

---

## 17. Success Criteria

The Smart Medicine Box will meet its core goals when:

- Medication can be scheduled using the application.
- The ESP32 can use the configured medication schedule.
- The physical medicine box provides reminders at the correct time.
- Medication-box activity can be detected and recorded.
- Medication status can be viewed through the application.
- The caregiver can receive relevant medication notifications.
- Missed medication can be identified and reported.
- The system provides a practical mechanism to improve medication safety and medication adherence.

---

## 18. Objective-to-Feature Mapping

| Project Objective | Supporting Features |
|---|---|
| **1. Improve medicine safety** | Medication scheduling, clear medicine identification, status monitoring, history, missed-dose detection |
| **2. Notify the caregiver through the app** | Caregiver account/dashboard, due alerts, confirmation updates, missed-medication notifications |
| **3. Ensure medicine is taken on time** | RTC/schedule checking, buzzer, LED, LCD reminder, mobile notification, missed-dose monitoring |

---

## 19. Final System Concept

```text
                 SMART MEDICINE BOX

             +--------------------+
             |     Mobile App     |
             | User + Caregiver   |
             +---------+----------+
                       |
                       v
             +--------------------+
             | Backend / Database |
             +---------+----------+
                       |
                       v
             +--------------------+
             |       ESP32        |
             | Arduino Firmware   |
             +---------+----------+
                       |
        +--------------+--------------+
        |              |              |
        v              v              v
   LCD/Buzzer/LED   IR Sensor      RTC/Time
        |              |              |
        +--------------+--------------+
                       |
                       v
                Medicine Reminder
                       |
                       v
                Status Monitoring
                       |
                       v
              Caregiver Notification
```

The final system is designed around the three core objectives: **improving medicine safety, notifying the caregiver through the application, and helping ensure medication is taken on time.**
