package com.kira_zina.haltprivacy.security;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class PolicyRecoveryWorker extends Worker {
    private static final String TAG = "PolicyRecoveryWorker";

    public PolicyRecoveryWorker(@NonNull android.content.Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        HardwareControlEngine engine = new HardwareControlEngine(getApplicationContext());

        if (!engine.isAdminActive()) {
            Log.w(TAG, "Admin not active after boot. Waiting for user to open app.");
            return Result.success();
        }

        if (engine.isDeviceOwner() || engine.isProfileOwner()) {
            engine.enableHardwarePrivacy();
            Log.i(TAG, "Hardware privacy re-applied after boot.");
            return Result.success();
        }

        Log.w(TAG, "Not provisioned after boot. Skipping — user must provision via app.");
        return Result.success();
    }
}
