# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

Patients managing daily medication and caregivers monitoring whether scheduled medicine-box activity was confirmed. The app may be used quickly on a phone when a dose is due, including by older adults.

## Product Purpose

Smart Medicine Box helps patients follow medication schedules and gives caregivers timely visibility into due, confirmed, and missed events. Success means the next required action is clear and medication status is never ambiguous.

## Positioning

The product coordinates a physical ESP32 medicine box with schedule management, box-activity confirmation, reminders, and caregiver monitoring. The Android app supports authenticated REST communication with the ESP32 over the local Wi-Fi network; operation remains local when the device is unavailable.

## Operating Context

Patients check the dashboard and respond to reminders throughout the day. Caregivers scan current status and recent history. The physical device uses Wi-Fi, an LCD, buzzer, LED, RTC, and IR sensor.

## Capabilities and Constraints

- Add, edit, and delete medication schedules.
- Show today's schedule, next medicine, history, and caregiver status.
- Support patient and caregiver roles.
- Treat IR sensor activity as box access or confirmation, not proof that medicine was swallowed.
- Preserve Android system navigation and Material 3 interaction conventions.
- Do not report the device as connected until a real backend or device transport confirms it.
- Synchronize medicine changes to a paired ESP32 and import confirmed or missed hardware events.

## Brand Commitments

The product name is Smart Medicine Box. The voice is calm, direct, and safety-focused. The confirmed visual direction is a calm healthcare interface with high contrast and strong legibility for older users.

## Evidence on Hand

Product requirements and system behavior are documented in `Smart_Medicine_Box_App_Documentation.md`. No clinical efficacy claims, customer evidence, or external brand assets are available and none should be fabricated.

## Product Principles

- Put the next medication action first.
- Distinguish pending, due, confirmed, and missed states without relying on color alone.
- Prefer accurate safety language over claims the sensor cannot prove.
- Keep primary tasks reachable and understandable at a glance.
- Preserve patient privacy and avoid exposing unnecessary information.

## Accessibility & Inclusion

Use Material 3 type roles, support Android font scaling and dark theme, provide text labels for icons and status, maintain at least 48 dp touch targets, and avoid color-only communication.
