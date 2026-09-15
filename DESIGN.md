---
name: Smart Medicine Box
description: A calm, safety-first Material 3 system for medication routines.
colors:
  medical-teal: "#006A6A"
  medical-teal-container: "#9CF1F0"
  calm-blue: "#4A6363"
  calm-blue-container: "#CCE8E7"
  healthy-green: "#466600"
  healthy-green-container: "#D8EFB8"
  app-background: "#F5FAF9"
  ink: "#171D1D"
  ink-muted: "#3F4948"
typography:
  headline:
    fontFamily: "sans-serif"
    fontSize: "30sp"
    fontWeight: 700
    lineHeight: "36sp"
  title:
    fontFamily: "sans-serif"
    fontSize: "16sp"
    fontWeight: 600
    lineHeight: "22sp"
  body:
    fontFamily: "sans-serif"
    fontSize: "16sp"
    fontWeight: 400
    lineHeight: "24sp"
  label:
    fontFamily: "sans-serif"
    fontSize: "14sp"
    fontWeight: 600
    lineHeight: "20sp"
rounded:
  small: "8dp"
  medium: "12dp"
  large: "20dp"
spacing:
  compact: "8dp"
  standard: "16dp"
  spacious: "24dp"
components:
  button-primary:
    backgroundColor: "{colors.medical-teal}"
    textColor: "#FFFFFF"
    rounded: "{rounded.medium}"
    height: "56dp"
  card-standard:
    backgroundColor: "{colors.app-background}"
    textColor: "{colors.ink}"
    rounded: "{rounded.medium}"
    padding: "16dp"
---

# Design System: Smart Medicine Box

## Overview

**Creative North Star: "The Quiet Medicine Tray"**

The interface borrows the order and tactile certainty of a well-organized medicine tray. It is calm enough for repeated daily use and explicit enough for time-sensitive action. Material 3 supplies all Android structure and behavior; the brand appears through cool healthcare tones, restrained elevation, and unusually clear status language.

**Key Characteristics:** safety-first hierarchy, cool neutral surfaces, teal primary actions, labeled status icons, and generous touch targets.

## Colors

Medical teal is the identifying color. Cool gray-green neutrals reduce glare, while Material semantic roles carry confirmation and error states in both themes.

**The Meaning Before Color Rule.** Every medication state includes a label and icon. Color reinforces meaning but never carries it alone.

## Typography

**Display Font:** Android sans-serif
**Body Font:** Android sans-serif

Use Material 3 typography roles rather than screen-specific font sizes. Headlines are bold but compact; body copy remains at least 14sp with comfortable line height.

## Layout

Compact phone layouts use a single column, 16dp horizontal margins, 8dp to 12dp gaps inside groups, and 20dp to 24dp separation between sections. Lists use `LazyColumn`. Primary actions remain at least 48dp high and content respects scaffold insets.

## Elevation & Depth

Depth is tonal first. Cards use Material surface-container roles and only low elevation where hierarchy requires it. Avoid decorative shadows and colored glows.

## Shapes

The shape scale is 8dp for compact status surfaces, 12dp for controls and standard cards, and 20dp for the single prominent next-medicine panel. Pill shapes are reserved for Material controls whose component behavior requires them.

## Components

### Buttons
- Filled teal buttons represent the primary action.
- Tonal, outlined, and text variants preserve Material state and focus behavior.
- Labels use direct verbs and stay on one line.

### Cards / Containers
- Standard records use `surfaceContainerLow`, 12dp corners, and 16dp padding.
- Only the next scheduled medicine receives the larger 20dp container and stronger tonal field.

### Navigation
- Use Material `NavigationBar` with four destinations on compact screens.
- Use one Material icon family and pair every icon with a visible label.

## Do's and Don'ts

### Do:
- **Do** put the next medication time and action before secondary information.
- **Do** use Material color and typography roles in components.
- **Do** describe sensor events as access or confirmation, not proven ingestion.

### Don't:
- **Don't** use emoji or Unicode symbols as interface icons.
- **Don't** use raw status colors inside screens when a semantic theme role applies.
- **Don't** communicate due, confirmed, or missed state through color alone.
