package com.KIRA_ZINA.haltprivacy.security;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.UserManager;
import android.util.Log;

/**
 * Engine responsible for applying security profiles and restricting hardware interfaces.
 * Adapts restriction scope based on Device Owner vs Profile Owner privileges.
 */
public class HardwareControlEngine {
    private static final String TAG = "HardwareControlEngine";
    private static final String PACKAGE_NAME = "com.KIRA_ZINA.haltprivacy";

    private final DevicePolicyManager devicePolicyManager;
    private final ComponentName adminComponent;
    private final Context context;

    public HardwareControlEngine(Context context) {
        this.context = context.getApplicationContext();
        this.devicePolicyManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        this.adminComponent = new ComponentName(context, EnterpriseSecurityReceiver.class);
    }

    /**
     * Enforces global or profile-scoped privacy mode by blocking camera layers 
     * and muting microphone controllers.
     */
    public void enableHardwarePrivacy() {
        if (!devicePolicyManager.isAdminActive(adminComponent)) {
            Log.e(TAG, "Execution failed: Component is not an active device administrator.");
            return;
        }

        try {
            // Step 1: Disable all physical camera hardware components
            if (isDeviceOwner() || isProfileOwner()) {
                devicePolicyManager.setCameraDisabled(adminComponent, true);
                Log.d(TAG, "Camera access disabled. Scope: " + (isDeviceOwner() ? "Global" : "Work Profile"));
            }

            // Step 2: Restrict user from unmuting the microphone interface
            if (isDeviceOwner()) {
                devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_UNMUTE_MICROPHONE);
                Log.d(TAG, "Microphone unmute restricted globally.");
            }

            // Step 3: Android 12 Specific Protection - Lock down privacy quick setting toggles
            if (isDeviceOwner()) {
                devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_CAMERA_TOGGLE);
                devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_MICROPHONE_TOGGLE);
                Log.d(TAG, "Android 12 privacy toggles locked out.");
            }

            // Step 4: Profile Owner specific restrictions (within work profile scope)
            if (isProfileOwner()) {
                devicePolicyManager.setCameraDisabled(adminComponent, true);
                // Note: DISALLOW_UNMUTE_MICROPHONE may have limited scope in Profile Owner mode
                devicePolicyManager.addUserRestriction(adminComponent, UserManager.DISALLOW_CAMERA_TOGGLE);
                Log.d(TAG, "Work Profile hardware restrictions applied.");
            }

        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException while enforcing hardware policies", e);
        }
    }

    /**
     * Lifts applied hardware restrictions, restoring camera and microphone capabilities.
     */
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

    /**
     * Validates if the device privacy lockdown is actively enforced.
     * Returns false safely if admin is not active or if SecurityException occurs.
     */
    public boolean isPrivacyModeEnforced() {
        // Prevent SecurityException if admin is not yet active
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
        return devicePolicyManager.isDeviceOwnerApp(PACKAGE_NAME);
    }

    public boolean isProfileOwner() {
        return devicePolicyManager.isProfileOwnerApp(PACKAGE_NAME);
    }

    public boolean isAdminActive() {
        return devicePolicyManager.isAdminActive(adminComponent);
    }

    /**
     * Enables tamper-proofing measures including uninstall blocking and kiosk mode.
     * Should be called after successful Device Owner provisioning.
     */
    public void enableTamperProofing() {
        if (!isDeviceOwner() || !devicePolicyManager.isAdminActive(adminComponent)) {
            Log.e(TAG, "Tamper-proofing requires Device Owner privileges.");
            return;
        }

        try {
            devicePolicyManager.setUninstallBlocked(adminComponent, PACKAGE_NAME, true);
            Log.d(TAG, "Application uninstall blocked.");

            devicePolicyManager.setLockTaskPackages(adminComponent, new String[]{PACKAGE_NAME});
            Log.d(TAG, "Lock task (kiosk) packages configured.");

        } catch (SecurityException e) {
            Log.e(TAG, "Failed to apply tamper-proofing measures", e);
        }
    }

    /**
     * Disables tamper-proofing measures when removing device restrictions.
     */
    public void disableTamperProofing() {
        if (!isDeviceOwner() || !devicePolicyManager.isAdminActive(adminComponent)) {
            return;
        }

        try {
            devicePolicyManager.setUninstallBlocked(adminComponent, PACKAGE_NAME, false);
            Log.d(TAG, "Application uninstall blocking removed.");

            devicePolicyManager.setLockTaskPackages(adminComponent, new String[]{});
            Log.d(TAG, "Lock task packages cleared.");

        } catch (SecurityException e) {
            Log.e(TAG, "Failed to disable tamper-proofing measures", e);
        }
    }
}