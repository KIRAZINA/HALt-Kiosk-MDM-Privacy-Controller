package com.kira_zina.haltprivacy.security;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Reacts to configuration changes pushed by the device management console.
 *
 * <p>The platform delivers
 * {@link Intent#ACTION_APPLICATION_RESTRICTIONS_CHANGED} whenever the
 * managed configuration of this application changes, for example because the
 * operator changed a policy in the EMM console.</p>
 */
public class RestrictionsReceiver extends BroadcastReceiver {
    private static final String TAG = "RestrictionsReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Managed configuration changed. Applying the remote policy.");

        ManagedConfigManager configManager = new ManagedConfigManager(context);
        ManagedConfig config = configManager.readManagedConfig();
        Log.i(TAG, "Received configuration: " + config);

        String summary = new ManagedPolicyApplier(context).apply(config);
        Log.i(TAG, summary);
    }
}
