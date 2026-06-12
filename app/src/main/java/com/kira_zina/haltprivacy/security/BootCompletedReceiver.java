package com.kira_zina.haltprivacy.security;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkManager;

import java.util.concurrent.Executors;

public class BootCompletedReceiver extends BroadcastReceiver {
    private static final String TAG = "BootCompletedReceiver";

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.i(TAG, "Boot completed. Scheduling policy recovery.");
            final PendingResult pendingResult = goAsync();

            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    Constraints constraints = new Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                            .build();

                    OneTimeWorkRequest recoveryWork =
                            new OneTimeWorkRequest.Builder(PolicyRecoveryWorker.class)
                                    .setConstraints(constraints)
                                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                                    .build();

                    WorkManager.getInstance(context).enqueue(recoveryWork).getResult().get();
                    Log.i(TAG, "Boot recovery work enqueued successfully.");
                } catch (Exception e) {
                    Log.e(TAG, "Failed to enqueue boot recovery work", e);
                } finally {
                    pendingResult.finish();
                }
            });
        }
    }
}
