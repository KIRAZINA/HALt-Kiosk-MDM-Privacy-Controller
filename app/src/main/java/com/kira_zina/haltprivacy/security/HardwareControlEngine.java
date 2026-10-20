package com.kira_zina.haltprivacy.security;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.UserManager;
import android.util.Log;

import com.kira_zina.haltprivacy.AppIdentity;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class HardwareControlEngine {
    private static final String TAG = "HardwareControlEngine";

    /**
     * Lock-task feature flags used for a fully locked-down kiosk session.
     *
     * <p>{@code LOCK_TASK_FEATURE_NONE} keeps every optional kiosk affordance
     * switched off: no notification shade, no home button, no system info
     * overlay and no keyguard escape hatch.</p>
     */
    public static final int KIOSK_LOCK_TASK_FEATURES = DevicePolicyManager.LOCK_TASK_FEATURE_NONE;

    private final DevicePolicyManager devicePolicyManager;
    private final ComponentName adminComponent;
    private final String packageName;

    public HardwareControlEngine(Context context) {
        Context appContext = context.getApplicationContext();
        this.packageName = AppIdentity.packageName(appContext);
        this.devicePolicyManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        this.adminComponent = new ComponentName(context, EnterpriseSecurityReceiver.class);
    }

    public void enableHardwarePrivacy() {
        if (!devicePolicyManager.isAdminActive(adminComponent)) {
            Log.e(TAG, "Execution failed: Component is not an active device administrator.");
            return;
        }

        try {
            boolean isDO = isDeviceOwner();
            boolean isPO = isProfileOwner();

            if (isDO || isPO) {
                devicePolicyManager.setCameraDisabled(adminComponent, true);
                Log.d(TAG, "Camera access disabled. Scope: " + (isDO ? "Global" : "Work Profile"));
            }

            if (isDO) {
                devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_UNMUTE_MICROPHONE);
                devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_CAMERA_TOGGLE);
                devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_MICROPHONE_TOGGLE);
                Log.d(TAG, "Android 12 privacy toggles locked out globally.");
            }

            if (isPO) {
                devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_CAMERA_TOGGLE);
                Log.d(TAG, "Work Profile hardware restrictions applied.");
            }

        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException while enforcing hardware policies", e);
        }
    }

    public void disableHardwarePrivacy() {
        if (!devicePolicyManager.isAdminActive(adminComponent)) {
            Log.e(TAG, "Execution failed: Component is not an active device administrator.");
            return;
        }

        try {
            devicePolicyManager.setCameraDisabled(adminComponent, false);
            devicePolicyManager.clearUserRestriction(adminComponent, UserManager.DISALLOW_UNMUTE_MICROPHONE);
            devicePolicyManager.clearUserRestriction(adminComponent, UserManager.DISALLOW_CAMERA_TOGGLE);
            devicePolicyManager.clearUserRestriction(adminComponent, UserManager.DISALLOW_MICROPHONE_TOGGLE);
            disableTamperProofing();
            Log.i(TAG, "Enterprise hardware privacy policies rolled back.");

        } catch (SecurityException e) {
            Log.e(TAG, "Failed to lift system hardware restrictions safely", e);
        }
    }

    public boolean isPrivacyModeEnforced() {
        if (!isAdminActive()) {
            Log.d(TAG, "Admin not active; privacy mode cannot be enforced yet.");
            return false;
        }

        try {
            return devicePolicyManager.getCameraDisabled(adminComponent);
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException while checking privacy mode: " + e.getMessage());
            return false;
        }
    }

    public boolean isDeviceOwner() {
        return devicePolicyManager.isDeviceOwnerApp(packageName);
    }

    public boolean isProfileOwner() {
        return devicePolicyManager.isProfileOwnerApp(packageName);
    }

    public boolean isAdminActive() {
        return devicePolicyManager.isAdminActive(adminComponent);
    }

    /**
     * Blocks removal of this application and makes it the only package allowed
     * to run in lock-task (kiosk) mode.
     *
     * @return true when every measure was applied
     */
    public boolean enableTamperProofing() {
        return enableTamperProofing(Collections.emptySet());
    }

    /**
     * Blocks removal of this application and registers the lock-task (kiosk)
     * packages, including the operator supplied secondary applications.
     *
     * @param additionalPackages secondary applications allowed to run alongside
     *                           this app; may be null
     * @return true when every measure was applied
     */
    public boolean enableTamperProofing(Set<String> additionalPackages) {
        if (!isAdminActive() || !isDeviceOwner()) {
            Log.e(TAG, "Tamper-proofing requires Device Owner privileges.");
            return false;
        }

        try {
            devicePolicyManager.setUninstallBlocked(adminComponent, packageName, true);
            Log.d(TAG, "Application uninstall blocked.");
        } catch (SecurityException e) {
            Log.e(TAG, "Failed to block application uninstall", e);
            return false;
        }

        return enableKioskMode(additionalPackages);
    }

    /**
     * Removes the uninstall block and clears the lock-task (kiosk) configuration.
     *
     * @return true when every measure was rolled back
     */
    public boolean disableTamperProofing() {
        if (!isAdminActive() || !isDeviceOwner()) {
            Log.w(TAG, "Cannot roll back tamper-proofing without Device Owner privileges.");
            return false;
        }

        try {
            devicePolicyManager.setUninstallBlocked(adminComponent, packageName, false);
            Log.d(TAG, "Application uninstall blocking removed.");
        } catch (SecurityException e) {
            Log.e(TAG, "Failed to remove application uninstall block", e);
            return false;
        }

        return disableKioskMode();
    }

    /**
     * Registers this package as a lock-task (kiosk) package and applies a
     * fully locked-down kiosk feature set.
     *
     * @return true when the lock-task configuration was applied
     */
    public boolean enableKioskMode() {
        return enableKioskMode(Collections.emptySet());
    }

    /**
     * Registers the lock-task (kiosk) packages and applies a fully locked-down
     * kiosk feature set.
     *
     * <p>The application's own package is always part of the registered set, so
     * the session can never lock the device out of its own management console.
     * Operator supplied entries that are blank or that repeat an existing
     * package are ignored.</p>
     *
     * <p>Both calls are Device Owner only. A failure on either leaves the
     * device in a partially configured state, which is reported as
     * {@code false} so the caller can surface it.</p>
     *
     * @param additionalPackages secondary applications allowed to run alongside
     *                           this app; may be null
     * @return true when the lock-task configuration was applied
     */
    public boolean enableKioskMode(Set<String> additionalPackages) {
        if (!isAdminActive() || !isDeviceOwner()) {
            Log.e(TAG, "Kiosk mode requires an active Device Owner.");
            return false;
        }

        try {
            String[] packages = buildLockTaskPackages(additionalPackages);
            devicePolicyManager.setLockTaskPackages(adminComponent, packages);
            Log.d(TAG, "Lock task (kiosk) packages configured: " + Arrays.toString(packages));

            devicePolicyManager.setLockTaskFeatures(adminComponent, KIOSK_LOCK_TASK_FEATURES);
            Log.d(TAG, "Lock task features set to NONE (fully locked-down kiosk).");

            return true;
        } catch (SecurityException e) {
            Log.e(TAG, "Failed to configure kiosk (lock task) mode", e);
            return false;
        }
    }

    /**
     * Builds the lock-task package array: this application first, followed by
     * the operator supplied secondary applications in iteration order.
     *
     * @param additionalPackages secondary applications; may be null
     * @return the package array handed to the device policy manager
     */
    private String[] buildLockTaskPackages(Set<String> additionalPackages) {
        Set<String> packages = new LinkedHashSet<>();
        packages.add(packageName);

        if (additionalPackages != null) {
            for (String candidate : additionalPackages) {
                if (candidate == null) {
                    continue;
                }
                String trimmed = candidate.trim();
                if (!trimmed.isEmpty()) {
                    packages.add(trimmed);
                }
            }
        }

        return packages.toArray(new String[0]);
    }

    /**
     * Returns the packages currently registered for lock-task (kiosk) mode.
     *
     * <p>The result contains this application's own package plus every
     * secondary application that was allowed to run alongside it.</p>
     *
     * @return the registered lock-task packages, never null
     */
    public Set<String> getAllowedKioskPackages() {
        if (!isAdminActive()) {
            return Collections.emptySet();
        }

        try {
            String[] packages = devicePolicyManager.getLockTaskPackages(adminComponent);
            if (packages == null) {
                return Collections.emptySet();
            }
            return new LinkedHashSet<>(Arrays.asList(packages));
        } catch (SecurityException e) {
            Log.e(TAG, "Failed to read lock task packages", e);
            return Collections.emptySet();
        }
    }

    /**
     * Clears the lock-task (kiosk) configuration and resets the kiosk feature
     * flags back to the platform default.
     *
     * @return true when the lock-task configuration was cleared
     */
    public boolean disableKioskMode() {
        if (!isAdminActive() || !isDeviceOwner()) {
            Log.e(TAG, "Kiosk mode can only be cleared by an active Device Owner.");
            return false;
        }

        try {
            devicePolicyManager.setLockTaskPackages(adminComponent, new String[]{});
            Log.d(TAG, "Lock task packages cleared.");

            devicePolicyManager.setLockTaskFeatures(adminComponent, DevicePolicyManager.LOCK_TASK_FEATURE_NONE);
            Log.d(TAG, "Lock task features reset to NONE.");

            return true;
        } catch (SecurityException e) {
            Log.e(TAG, "Failed to clear kiosk (lock task) configuration", e);
            return false;
        }
    }

    /**
     * Reports whether this package is currently registered as a lock-task
     * (kiosk) package on the device.
     *
     * @return true when the device policy manager lists this package
     */
    public boolean isKioskModeConfigured() {
        if (!isAdminActive()) {
            return false;
        }

        try {
            String[] packages = devicePolicyManager.getLockTaskPackages(adminComponent);
            if (packages == null) {
                return false;
            }
            for (String candidate : packages) {
                if (packageName.equals(candidate)) {
                    return true;
                }
            }
            return false;
        } catch (SecurityException e) {
            Log.e(TAG, "Failed to read lock task packages", e);
            return false;
        }
    }

    /**
     * Starts a lock-task (kiosk) session for the given activity.
     *
     * <p>Only the foreground activity can enter lock-task mode, so this helper
     * is invoked by the Activity in response to a ViewModel request. The
     * package must already be registered through {@link #enableKioskMode()},
     * otherwise the platform rejects the request.</p>
     *
     * @param activity the activity to lock
     * @return true when a lock-task session is active
     */
    public static boolean startLockTaskSession(Activity activity) {
        if (activity == null) {
            Log.w(TAG, "Cannot start lock task session: activity is null.");
            return false;
        }

        if (isLockTaskActive(activity)) {
            return true;
        }

        try {
            activity.startLockTask();
            Log.i(TAG, "Lock task (kiosk) session started.");
            return true;
        } catch (IllegalStateException | SecurityException e) {
            Log.e(TAG, "Failed to start lock task session", e);
            return false;
        }
    }

    /**
     * Ends the lock-task (kiosk) session of the given activity.
     *
     * @param activity the activity to unlock
     * @return true when no lock-task session is active
     */
    public static boolean stopLockTaskSession(Activity activity) {
        if (activity == null) {
            Log.w(TAG, "Cannot stop lock task session: activity is null.");
            return false;
        }

        if (!isLockTaskActive(activity)) {
            return true;
        }

        try {
            activity.stopLockTask();
            Log.i(TAG, "Lock task (kiosk) session stopped.");
            return true;
        } catch (IllegalStateException | SecurityException e) {
            Log.e(TAG, "Failed to stop lock task session", e);
            return false;
        }
    }

    /**
     * @param activity the activity to inspect
     * @return true when the device currently runs in lock-task mode
     */
    public static boolean isLockTaskActive(Activity activity) {
        if (activity == null) {
            return false;
        }

        ActivityManager activityManager =
                (ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE);
        return activityManager != null
                && activityManager.getLockTaskModeState() == ActivityManager.LOCK_TASK_MODE_LOCKED;
    }
}
