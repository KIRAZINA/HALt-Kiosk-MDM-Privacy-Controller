package com.kira_zina.haltprivacy.security;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.kira_zina.haltprivacy.provisioning.ProvisioningEngine;

import java.util.concurrent.TimeUnit;

public class ReapplyPolicyWorker extends Worker {
    private static final String TAG = "ReapplyPolicyWorker";
    private static final String WORK_NAME = "reapply_policy";

    public ReapplyPolicyWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.i(TAG, "Re-applying enterprise hardware policies after boot.");
        HardwareControlEngine engine = new HardwareControlEngine(getApplicationContext());

        if (!engine.isAdminActive()) {
            Log.w(TAG, "No active device admin found. Initiating provisioning cascade.");
            ProvisioningEngine provisioningEngine = new ProvisioningEngine(getApplicationContext());
            provisioningEngine.executeProvisioning();
        }

        if (engine.isAdminActive() && (engine.isDeviceOwner() || engine.isProfileOwner())) {
            engine.enableHardwarePrivacy();
            Log.i(TAG, "Hardware privacy policies re-applied successfully.");
            return Result.success();
        } else {
            Log.w(TAG, "Admin not active after boot; privacy mode cannot be enforced yet.");
            return Result.retry();
        }
    }

    public static void enqueue(@NonNull Context context) {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ReapplyPolicyWorker.class)
                .setInitialDelay(3, TimeUnit.SECONDS)
                .addTag("enterprise_policy")
                .build();

        WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request);
    }
}
