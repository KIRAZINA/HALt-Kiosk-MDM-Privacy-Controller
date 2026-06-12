package com.kira_zina.haltprivacy;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Configuration;

public class SecureServiceApplication extends Application implements Configuration.Provider {
    private static final String TAG = "HALtPrivacyApp";

    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.INFO)
                .build();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "HALt Privacy Application initialized.");

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                Log.e(TAG, "Uncaught exception in thread " + thread.getName(), throwable);
                java.io.File out = new java.io.File(getFilesDir(), "last_crash.txt");
                try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter(out, false))) {
                    throwable.printStackTrace(pw);
                } catch (Exception e) {
                    Log.w(TAG, "Failed to write crash file", e);
                }
            } catch (Throwable t) {
                // swallow
            } finally {
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(10);
            }
        });
    }
}
