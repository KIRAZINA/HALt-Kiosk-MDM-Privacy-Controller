package com.kira_zina.haltprivacy.security;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.UserManager;
import android.util.Log;

public class HardwareControlEngine {
    private static final String TAG = "HardwareControlEngine";
    private static final String PACKAGE_NAME = "com.kira_zina.haltprivacy";

    private final DevicePolicyManager devicePolicyManager;
    private final ComponentName adminComponent;
    private final Context context;

    public HardwareControlEngine(Context context) {
        this.context = context.getApplicationContext();
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
        return devicePolicyManager.isDeviceOwnerApp(PACKAGE_NAME);
    }

    public boolean isProfileOwner() {
        return devicePolicyManager.isProfileOwnerApp(PACKAGE_NAME);
    }

    public boolean isAdminActive() {
        return devicePolicyManager.isAdminActive(adminComponent);
    }

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
