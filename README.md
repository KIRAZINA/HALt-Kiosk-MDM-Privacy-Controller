# HALt Privacy: Enhanced Android Enterprise Blocker

## Overview
HALt Privacy is a production-ready **Device Policy Controller (DPC)** designed for high-security environments. It leverages Android Enterprise APIs to programmatically disable hardware components (Camera and Microphone) at the system level.

The application operates as a **Device Owner** (or **Profile Owner** fallback) to enforce non-revocable privacy policies, bypassing standard user controls and locking down Android 12+ privacy toggles.

## Features
*   **Automated Provisioning**: 
    *   **Root-Assisted**: Self-elevation via shell commands on rooted/debug builds.
    *   **QR/NFC**: Android Enterprise enrollment for factory-reset devices.
    *   **Profile Owner**: Fallback to Work Profile management if Device Owner is blocked.
*   **Hardware Lockdown**: 
    *   Global camera disabling via `DevicePolicyManager`.
    *   System-wide microphone muting and restriction.
    *   Android 12 Privacy Toggle interception (Camera/Mic access tiles).
*   **Tamper Proofing**:
    *   Persistence across reboots via `BootCompletedReceiver`.
    *   Prevention of uninstallation in Device Owner mode.
*   **MVVM Architecture**: Clean separation of UI and security logic.

## Technical Specifications
*   **Language**: Java 21
*   **Minimum SDK**: API 31 (Android 12)
*   **Target SDK**: API 36 (Android 16 preview)
*   **Build System**: Gradle Kotlin DSL

## Getting Started

### 1. Building the Project
Clone the repository and build the APK using Gradle:
```bash
./gradlew assembleDebug
```

### 2. Provisioning Pathways

#### Pathway A: ADB/Root (No Factory Reset)
If the device is rooted or has ADB shell access enabled:
1. Install the APK: `adb install app-debug.apk`
2. Open the app and tap **Auto-Provision**.

#### Pathway B: QR Code (Factory Reset)
For new or wiped devices:
1. Tap the setup screen 6 times to launch the QR scanner.
2. Scan a QR code containing the `EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME` for `com.KIRA_ZINA.haltprivacy.security.EnterpriseSecurityReceiver`.

### 3. Usage
*   **Enable Lockdown**: Disables all cameras and restricts the microphone.
*   **Disable Lockdown**: Restores hardware access.
*   **Refresh Status**: Manually query the current privilege level and policy enforcement state.

## Project Structure
*   `provisioning/`: Logic for DPC enrollment and privilege escalation.
*   `security/`: Hardware control logic and broadcast receivers (Admin/Boot).
*   `ui/`: Android Activity and ViewModel implementation.

## Security Warning
Standard Android security prevents setting a Device Owner if accounts (Google, Exchange, etc.) already exist on the device. For full global lockdown on non-rooted devices, a **factory reset** is required.

## Author
KIRAZINA

## License
Confidential - For Enterprise Security Evaluation only.
