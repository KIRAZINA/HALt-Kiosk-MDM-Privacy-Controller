package com.KIRA_ZINA.haltprivacy.security;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.KIRA_ZINA.haltprivacy.provisioning.ProvisioningEngine;

/**
 * Receiver ensuring hardware restrictions are re-applied after system reboot.
 * Also triggers provisioning cascade if no active admin exists.
 */
public class BootCompletedReceiver extends BroadcastReceiver {
    private static final String TAG = "BootCompletedReceiver";

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.i(TAG, "System boot detected. Re-applying enterprise hardware policies.");
            HardwareControlEngine engine = new HardwareControlEngine(context);

            if (!engine.isAdminActive()) {
                Log.w(TAG, "No active device admin found. Initiating provisioning cascade.");
                ProvisioningEngine provisioningEngine = new ProvisioningEngine(context);
                provisioningEngine.executeProvisioning();
            }

            if (engine.isAdminActive() && (engine.isDeviceOwner() || engine.isProfileOwner())) {
                engine.enableHardwarePrivacy();
            } else {
                Log.w(TAG, "Admin not active after boot; privacy mode cannot be enforced yet.");
            }
        }
    }
}