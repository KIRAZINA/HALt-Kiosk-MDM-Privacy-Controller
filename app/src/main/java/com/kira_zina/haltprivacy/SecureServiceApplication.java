package com.kira_zina.haltprivacy;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Configuration;

import com.kira_zina.haltprivacy.security.TamperDetection;

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

        enforceTrustedBinary();
    }

    /**
     * Refuses to continue running when the APK signature cannot be trusted.
     *
     * <p>The check is a no-op for debug builds and for release builds that have
     * not yet had {@link TamperDetection#EXPECTED_SIGNATURE_HASH} configured;
     * both cases log a warning and continue. When a signature mismatch is
     * detected in an enforcing build, initialization is aborted: the process
     * must not run a device policy controller that was signed by an unknown
     * party.</p>
     */
    private void enforceTrustedBinary() {
        if (TamperDetection.verifyAppSignature(this)) {
            return;
        }

        Log.e(TAG, "CRITICAL: APK signature verification FAILED. "
                + "Refusing to initialize — this build may have been tampered with.");
        throw new SecurityException(
                "HALt Privacy: APK signature verification failed. Initialization aborted.");
    }
}
