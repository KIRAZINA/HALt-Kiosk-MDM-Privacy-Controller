package com.KIRA_ZINA.haltprivacy;

import android.app.Application;
import android.util.Log;

/**
 * Main Application class for HALt Privacy.
 * Serves as the global entry point for the application lifecycle.
 */
public class SecureServiceApplication extends Application {
    private static final String TAG = "HALtPrivacyApp";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "HALt Privacy Application initialized.");

        // Set a global uncaught exception handler to capture crashes that occur
        // before an activity-level handler can run. This will write the stack
        // trace to logcat and to a file in the app files directory for later
        // inspection by the developer.
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                Log.e(TAG, "Uncaught exception in thread " + thread.getName(), throwable);
                // Attempt to persist the stack trace to a file for remote debugging
                java.io.File out = new java.io.File(getFilesDir(), "last_crash.txt");
                try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter(out, false))) {
                    throwable.printStackTrace(pw);
                } catch (Exception e) {
                    Log.w(TAG, "Failed to write crash file", e);
                }
            } catch (Throwable t) {
                // Swallow any exceptions thrown while trying to log the original one
            } finally {
                // Let the default system handler proceed (this will still crash the app),
                // but we have captured the trace to a file and logcat which helps debugging.
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(10);
            }
        });
    }
}