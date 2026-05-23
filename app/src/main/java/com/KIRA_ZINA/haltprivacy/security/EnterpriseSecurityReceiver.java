package com.KIRA_ZINA.haltprivacy.security;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

/**
 * Receiver handling system-level device administration events and policy lifecycle changes.
 * Automatically triggers hardware lockdown upon successful provisioning.
 */
public class EnterpriseSecurityReceiver extends DeviceAdminReceiver {
    private static final String TAG = "EnterpriseSecurityCore";

    @Override
    public void onEnabled(@NonNull Context context, @NonNull Intent intent) {
        super.onEnabled(context, intent);
        Log.i(TAG, "Device Administration status: ENABLED");

        HardwareControlEngine engine = new HardwareControlEngine(context);
        if (engine.isDeviceOwner() || engine.isProfileOwner()) {
            engine.enableHardwarePrivacy();
            if (engine.isDeviceOwner()) {
                engine.enableTamperProofing();
            }
        }
    }

    @Override
    public void onDisabled(@NonNull Context context, @NonNull Intent intent) {
        super.onDisabled(context, intent);
        Log.w(TAG, "Warning: Device Administration status: DISABLED");
    }

    @Override
    public void onProfileProvisioningComplete(@NonNull Context context, @NonNull Intent intent) {
        super.onProfileProvisioningComplete(context, intent);
        Log.i(TAG, "Profile provisioning completed. Applying hardware restrictions.");

        HardwareControlEngine engine = new HardwareControlEngine(context);
        engine.enableHardwarePrivacy();
    }

    @Override
    public void onLockTaskModeEntering(@NonNull Context context, @NonNull Intent intent, @NonNull String pkg) {
        super.onLockTaskModeEntering(context, intent, pkg);
        Log.d(TAG, "Kiosk/Lock Task mode entering for package: " + pkg);
    }
}