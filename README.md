# HALt Kiosk MDM & Privacy Controller

## Overview
HALt Kiosk MDM & Privacy Controller is a **production-grade Android Enterprise Device Policy Controller (DPC)**. It combines three capabilities in a single device-policy application:

*   **Hardware privacy controls** — the camera is disabled through `DevicePolicyManager`, microphone unmuting is blocked, and the Android 12+ Quick Settings privacy tiles are locked out, so the user cannot undo the policy from the system UI.
*   **A secure multi-app Kiosk Mode** — the app registers itself, plus an allowlist of secondary applications, as the only packages permitted to run inside a lock-task (kiosk) session, and drives the session from its operator console.
*   **An EMM managed-configuration control plane** — a device management console can push policy to the DPC through an Android Enterprise restriction schema, and the app enforces that policy both when it is pushed and whenever it is opened.

The application operates as a **Device Owner**, with **Profile Owner** as a reduced-scope fallback, so the same build enforces device-wide lockdown on managed devices and work-profile-scoped lockdown everywhere else.

## Features

### Hardware Lockdown
*   **Camera disabling** via `DevicePolicyManager.setCameraDisabled` — device-wide as Device Owner, work-profile scoped as Profile Owner.
*   **Microphone unmute restriction** via `UserManager.DISALLOW_UNMUTE_MICROPHONE` (Device Owner only). The microphone is not muted; unmuting it is prevented.
*   **Android 12+ privacy toggle lockout** via `DISALLOW_CAMERA_TOGGLE` and `DISALLOW_MICROPHONE_TOGGLE` (Device Owner only); `DISALLOW_CAMERA_TOGGLE` is also applied in the Profile Owner scope.
*   All policy mutations are guarded by an active-administrator check and a Device Owner / Profile Owner role check, and every `DevicePolicyManager` call is contained by a `SecurityException` handler.

### Kiosk Mode (LockTask)
*   Registers the application — and an optional allowlist of secondary apps — as the only lock-task packages through `setLockTaskPackages`.
*   Applies `LOCK_TASK_FEATURE_NONE` through `setLockTaskFeatures`: no *additional* system affordances are blocked, so the session confines the user to the allowlisted packages while the status bar, notifications, home/overview buttons and the keyguard keep their normal behaviour.
*   The application's own package is **always** part of the registered set, so a kiosk session can never lock the device out of its own management console.
*   Allowlist entries are validated by `AppIdentity.isValidPackageName` — at least two dot-separated segments, each starting with a letter or underscore, at most 255 characters. Invalid names and duplicates are rejected.
*   The live session state is read back from `ActivityManager.getLockTaskModeState()` and shown in the System Status card; the indicator re-syncs on every window-focus change.

### Strict Timed Lockout
*   A **reboot-persistent wall-clock deadline** (`lockout_end_timestamp_ms`) is persisted in `SharedPreferences`, so the prohibition outlives the process and the device.
*   While the deadline is in the future, the lockdown and the kiosk configuration **cannot be disabled**: the controls are greyed out in the UI and the action is refused in the ViewModel with *"Cannot disable: Timed lockout active for remaining X min"*.
*   A one-second `CountDownTimer` publishes the remaining time (`01h 05m 30s` / `05m 30s`) to the console.
*   `PolicyRecoveryWorker` re-applies the kiosk configuration on the next boot whenever it finds an active deadline.
*   Preset durations of **15 minutes**, **30 minutes** and **1 hour** are available. When the deadline expires the *prohibition* is lifted — the policy itself stays enforced until it is removed explicitly.

### Managed Configurations (EMM)
*   Declares its own Android Enterprise restriction schema in `res/xml/app_restrictions.xml`, registered with the platform through the `android.content.APP_RESTRICTIONS` meta-data, so a console knows exactly which keys it may push:

    | Key | Type | Effect |
    |---|---|---|
    | `hardware_privacy_enabled` | `bool` | Enforce the camera and microphone restrictions |
    | `kiosk_mode_enabled` | `bool` | Register the lock-task packages and apply the kiosk feature set |
    | `kiosk_allowed_packages` | `multi-select` | The secondary applications allowed inside the kiosk session |
    | `timed_lockout_duration_minutes` | `integer` | Duration of the timed lockout; `0` disables it |

*   `ManagedConfigManager` reads the bundle from the protected `RestrictionsManager` and parses it type-tolerantly: a missing, empty or wrongly typed restriction degrades to its safe default instead of failing, so nothing is enforced unless the console explicitly asked for it.
*   `RestrictionsReceiver` listens for `ACTION_APPLICATION_RESTRICTIONS_CHANGED` and re-applies the policy immediately. The broadcast payload is ignored — the authoritative bundle is re-read — so a spoofed broadcast is harmless.
*   `ManagedPolicyApplier` turns a parsed configuration into device policy through the same engine the operator console uses. There is no second enforcement path.
*   The policy is also re-synced on every foreground transition, so a configuration pushed while the app was closed is enforced the next time it is opened.

### Tamper Detection
*   `TamperDetection` compares the SHA-256 of the installed APK's signing certificate against `TamperDetection.EXPECTED_SIGNATURE_HASH`, handling both multi-signer APKs and single-signer APKs with a rotation history.
*   The check is wired into `SecureServiceApplication.onCreate()`, so it runs on every process start. A mismatch in an enforcing build logs a critical error and aborts initialization rather than running as an untrusted device policy controller.
*   Debug builds, and release builds that still carry the placeholder hash, bypass the check with a prominent warning in logcat so that development is never blocked by a value that only exists after the release key exists.

### Enterprise Provisioning
*   `ProvisioningActivity` completes the Android Enterprise enrollment handshake. It reads the wizard's intent, distinguishes a genuine handshake from a manual launch, confirms the granted role, enforces the privacy and kiosk policy, and closes the handshake with an explicit `RESULT_OK` / `RESULT_CANCELED`.
*   Because the device administrator is activated asynchronously, the role is **polled every 300 ms for up to 15 seconds** before the handshake is given up on.
*   A manual launch of the activity is guarded: it reports `RESULT_CANCELED` and redirects to the operator console instead of pretending to be an enrollment.
*   `EnterpriseSecurityReceiver` reacts to the device-admin lifecycle callbacks and enforces the policy as soon as the administrator is activated.

### Architecture
*   **MVVM** with an AndroidX `LiveData`-based state holder; the policy execution lives in a single adapter over `DevicePolicyManager`.
*   **No network code**: the app makes no HTTP requests and declares no `INTERNET` permission. The only remote input is the managed configuration, delivered by the platform.

## Technical Specifications
*   **Language**: Java 21
*   **Minimum SDK**: API 31 (Android 12)
*   **Compile / Target SDK**: API 35 (Android 15)
*   **Build System**: Gradle Kotlin DSL (AGP 9.2.1 / Gradle 9.4.1)
*   **Application ID**: `com.kira_zina.haltprivacy`
*   **Device Admin Component**: `com.kira_zina.haltprivacy.security.EnterpriseSecurityReceiver`

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
3. The wizard launches `ProvisioningActivity`, which confirms the granted role, applies the
   privacy policy and returns `RESULT_OK` to complete the enrolment.

#### Pathway B: In-app provisioning trigger
On an already-running device, tap **Auto-Provision** in the app. The app first attempts
managed-device (Device Owner) enrolment, then falls back to managed-profile (Profile Owner)
enrolment, and finally reports failure if neither is available on the device.

> Provisioning requires the system enrolment screens, so a factory reset may be needed
> before Device Owner status can be obtained. The app does **not** attempt root or shell
> based self-elevation.

#### The enrollment handshake
`ProvisioningActivity` is the DPC's half of the enrolment transaction:

1. The wizard launches the activity with `ACTION_PROVISION_MANAGED_DEVICE`.
2. The activity verifies that the intent really came from the wizard (by action or by the
   `EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME` extra). A manual launch is redirected to
   the operator console with `RESULT_CANCELED`.
3. It polls the device policy manager every 300 ms for the Device Owner or Profile Owner
   role, with a 15 second timeout.
4. On success it applies the hardware privacy policy — and the kiosk configuration when the
   app is Device Owner — and closes the handshake with `RESULT_OK`.
5. On timeout it reports `RESULT_CANCELED` and shows *"The enrollment could not be confirmed."*

### 3. Usage
*   **Enable Lockdown**: Disables the camera and applies the microphone / privacy-toggle restrictions.
*   **Disable Lockdown**: Restores hardware access and clears the restrictions.
*   **Enable Kiosk Mode**: Blocks uninstallation, registers the app plus the allowlisted secondary apps as lock-task packages and enters the lock-task session (Device Owner only).
*   **Disable Kiosk Mode**: Leaves the lock-task session and clears the kiosk configuration.
*   **Kiosk Allowlist**: Type a package name (for example `com.android.chrome`) and tap **Add** to allow it inside the kiosk session; tap the close icon on a chip to remove it again. Invalid package names are rejected, and this app is always allowed.
*   **Start Timed Kiosk**: Pick a duration (15m / 30m / 1h) and start a timed lockout. The policy is applied immediately and cannot be disabled until the countdown reaches zero.
*   **Status Refresh**: The privilege level, the enforcement state, the kiosk configuration and the managed configuration are re-read every time the app returns to the foreground.

## Testing & Quality Assurance
The project ships **106 JVM unit tests** across **9 test classes**, executed with
`./gradlew test`:

| Test class | Tests | Coverage |
|---|---|---|
| `HardwareControlEngineLogicTest` | 32 | Policy rules, role gates, LockTask package/feature configuration, allowlist composition |
| `TimedLockoutControllerTest` | 14 | Deadline arithmetic, remaining time, duration formatting |
| `ProvisioningViewModelTest` | 14 | Timed-lockout start, the disable guard, allowlist management, status refresh |
| `ManagedConfigManagerTest` | 13 | Managed configuration parsing: null, empty, populated and malformed bundles |
| `PrefsManagerTest` | 10 | Preference persistence, including the lockout deadline |
| `TamperDetectionTest` | 9 | Signature verification, debug and unset-placeholder bypass, SHA-256 helper |
| `AppIdentityTest` | 6 | Package-name validation rule |
| `ProvisioningEngineLogicTest` | 6 | Role detection, account blocking, enrolment intent creation |
| `ExampleUnitTest` | 2 | Enumeration shape |
| **Total** | **106** | |

Highlights:
*   **No Robolectric.** The suite runs on a plain JVM. `unitTests.isReturnDefaultValues = true`
    lets the tests call stubbed Android APIs, and `androidx.arch.core:core-testing` supplies
    `InstantTaskExecutorRule` so that `LiveData.postValue` is synchronous on the test thread.
*   **No test-only production hooks.** The `DevicePolicyManager`, the `RestrictionsManager`, the
    `AccountManager` and the `SharedPreferences` are all mocked at the platform boundary, which
    is the same seam the production code uses.
*   The `ViewModel` is constructed through a `@VisibleForTesting` constructor that accepts its
    collaborators, so the orchestrator — the component with the most branching logic — is
    covered without an emulator.
*   A single instrumented test asserts the application id on a device.

Run everything with:
```bash
./gradlew test
```

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
*   `provisioning/`: Enrolment intent construction, privilege detection and the enrollment handshake.
*   `security/`: Hardware control and kiosk policy, broadcast receivers (Admin / Boot / Restrictions), background workers, kiosk allowlisting, timed lockout and tamper detection.
*   `ui/`: The operator console — Android Activity and ViewModel.
*   `AppIdentity`: Single source of truth for the application package name used in device-owner role checks, and the shared package-name validation rule.

## Security Warning
Standard Android security prevents setting a Device Owner if accounts (Google, Exchange, etc.) already exist on the device. The app checks for blocking Google accounts before provisioning and logs a warning, but the check cannot prevent the platform from rejecting the enrolment. For full global lockdown on non-rooted devices, a **factory reset** is required.

The timed lockout is an in-application control: it prevents the operator console from lifting the policy and survives reboots, but it does not stop a user with physical access, root, or factory-reset capability from removing the application. The `setUninstallBlocked` device-owner policy is the actual anti-removal control.

## Author
KIRAZINA

## License
Confidential - For Enterprise Security Evaluation only.
