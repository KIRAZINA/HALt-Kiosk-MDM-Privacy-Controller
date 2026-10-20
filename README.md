# HALt Privacy: Enhanced Android Enterprise Blocker

## Overview
HALt Privacy is a **Device Policy Controller (DPC)** designed for high-security environments. It leverages Android Enterprise APIs to programmatically disable hardware components (Camera) and restrict microphone access at the system level.

The application operates as a **Device Owner** (or **Profile Owner** fallback) to enforce privacy policies that bypass standard user controls and lock down Android 12+ privacy toggles.

## Features
*   **Automated Provisioning**:
    *   **QR/NFC**: Android Enterprise enrollment for factory-reset devices.
    *   **Profile Owner**: Fallback to Work Profile management if Device Owner is blocked.
*   **Hardware Lockdown**:
    *   Global camera disabling via `DevicePolicyManager` (device-wide as Device Owner, work-profile scoped as Profile Owner).
    *   Microphone unmute restriction via `UserManager.DISALLOW_UNMUTE_MICROPHONE` (Device Owner only).
    *   Android 12+ Privacy Toggle lockout via `DISALLOW_CAMERA_TOGGLE` and `DISALLOW_MICROPHONE_TOGGLE`.
*   **Tamper Proofing**:
    *   Persistence across reboots via `BootCompletedReceiver` and `PolicyRecoveryWorker`.
    *   Prevention of uninstallation in Device Owner mode (`setUninstallBlocked`).
    *   APK signing-certificate verification at startup (`TamperDetection`).
*   **Kiosk Mode (LockTask)**:
    *   Registers the app as a lock-task package and applies `LOCK_TASK_FEATURE_NONE` (no notifications, no home button, no system info, no keyguard escape).
    *   Secondary applications can be allowlisted to run alongside this app inside the kiosk session.
    *   Enters and leaves the lock-task session from the operator console; the live session state is shown in the System Status card.
*   **Timed Lockout (anti-bypass timer)**:
    *   Starts a strict lockout of 15 minutes, 30 minutes or 1 hour.
    *   While the timer runs, the lockdown and the kiosk configuration cannot be disabled — the controls are greyed out and the action is refused in the ViewModel.
    *   The deadline is persisted, so it survives a reboot: `PolicyRecoveryWorker` re-enforces the kiosk configuration on the next boot.
    *   A live countdown shows the remaining time; when it expires the policy stays in place but can be removed again.
*   **MVVM Architecture**: Clean separation of UI and security logic.
*   **Managed Configuration (Android Enterprise)**:
    *   Declares an EMM restriction schema in `res/xml/app_restrictions.xml`, so a device management console can push policy to the DPC.
    *   Supported keys: `hardware_privacy_enabled` (bool), `kiosk_mode_enabled` (bool), `kiosk_allowed_packages` (multi-select) and `timed_lockout_duration_minutes` (integer).
    *   A `RestrictionsReceiver` re-applies the policy whenever the console changes the configuration, and the policy is re-synced whenever the app is opened.
    *   Malformed or missing values fall back to the safe default and are never allowed to crash the parser.

## Technical Specifications
*   **Language**: Java 21
*   **Minimum SDK**: API 31 (Android 12)
*   **Compile / Target SDK**: API 35 (Android 15)
*   **Build System**: Gradle Kotlin DSL (AGP 9.2.1 / Gradle 9.4.1)
*   **Application ID**: `com.kira_zina.haltprivacy`

## Getting Started

### 1. Building the Project
Clone the repository and build the APK using Gradle:
```bash
./gradlew assembleDebug
```

### 2. Provisioning Pathways

#### Pathway A: Android Enterprise QR / NFC (factory reset)
For new or wiped devices:
1. Enrol the device with an Android Enterprise QR code or NFC tag that names
   `com.kira_zina.haltprivacy.security.EnterpriseSecurityReceiver` as the device admin.
2. The enrolment payload is produced by the device-management console, not by this app.
3. Once enrolment completes, the app enables the privacy lockdown automatically.

#### Pathway B: In-app provisioning trigger
On an already-running device, tap **Auto-Provision** in the app. The app first attempts
managed-device (Device Owner) enrolment, then falls back to managed-profile (Profile Owner)
enrolment, and finally reports failure if neither is available on the device.

> Provisioning requires the system enrolment screens, so a factory reset may be needed
> before Device Owner status can be obtained. The app does **not** attempt root or shell
> based self-elevation.

### 3. Usage
*   **Enable Lockdown**: Disables the camera and applies the microphone / privacy-toggle restrictions.
*   **Disable Lockdown**: Restores hardware access and clears the restrictions.
*   **Enable Kiosk Mode**: Blocks uninstallation, registers the app plus the allowlisted secondary apps as lock-task packages and enters the lock-task session (Device Owner only).
*   **Disable Kiosk Mode**: Leaves the lock-task session and clears the kiosk configuration.
*   **Kiosk Allowlist**: Type a package name (for example `com.android.chrome`) and tap **Add** to allow it inside the kiosk session; tap the close icon on a chip to remove it again. Invalid package names are rejected, and this app is always allowed.
*   **Start Timed Kiosk**: Pick a duration (15m / 30m / 1h) and start a timed lockout. The policy is applied immediately and cannot be disabled until the countdown reaches zero.
*   **Status Refresh**: The privilege level and enforcement state are re-read from the
    device policy manager every time the app returns to the foreground.

## Release Signing & Tamper Detection
`TamperDetection` compares the SHA-256 of the installed APK's signing certificate against
`TamperDetection.EXPECTED_SIGNATURE_HASH`. To enforce the check in a release build:

1. Obtain the SHA-256 of the release signing certificate.
2. Replace the `"YOUR_64_CHAR_HEX_HASH_HERE"` placeholder in
   `app/src/main/java/com/kira_zina/haltprivacy/security/TamperDetection.java` with the
   64-character uppercase hex value.
3. Rebuild the release APK.

Until that value is configured — and in all debug builds — verification is bypassed with a
prominent warning in logcat, so development is not blocked. If a release build with a
configured hash detects a signature mismatch, the app logs a critical error and aborts
initialization rather than running as an untrusted device policy controller.

## Project Structure
*   `provisioning/`: Logic for DPC enrollment and privilege detection.
*   `security/`: Hardware control logic, broadcast receivers (Admin/Boot/Restrictions), background workers, kiosk allowlisting, timed lockout and tamper detection.
*   `ui/`: Android Activity and ViewModel implementation.
*   `AppIdentity`: Single source of truth for the application package name used in device-owner role checks.

## Security Warning
Standard Android security prevents setting a Device Owner if accounts (Google, Exchange, etc.) already exist on the device. The app checks for blocking Google accounts before provisioning and logs a warning, but the check cannot prevent the platform from rejecting the enrolment. For full global lockdown on non-rooted devices, a **factory reset** is required.

## Author
KIRAZINA

## License
Confidential - For Enterprise Security Evaluation only.
