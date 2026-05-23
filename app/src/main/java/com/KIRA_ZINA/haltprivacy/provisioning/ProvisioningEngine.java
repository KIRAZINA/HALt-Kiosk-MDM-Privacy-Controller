package com.KIRA_ZINA.haltprivacy.provisioning;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.UserManager;
import android.util.Log;

import com.KIRA_ZINA.haltprivacy.security.EnterpriseSecurityReceiver;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * Engine responsible for automated privilege escalation to Device Owner or Profile Owner.
 * Implements a cascading fallback strategy for enterprise deployment scenarios.
 */
public class ProvisioningEngine {
    private static final String TAG = "ProvisioningEngine";
    private static final String PACKAGE_NAME = "com.KIRA_ZINA.haltprivacy";
    private static final String RECEIVER_NAME = "com.KIRA_ZINA.haltprivacy.security.EnterpriseSecurityReceiver";

    private final DevicePolicyManager dpm;
    private final ComponentName adminComponent;
    private final Context context;

    public ProvisioningEngine(Context context) {
        this.context = context.getApplicationContext();
        this.dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        this.adminComponent = new ComponentName(context, EnterpriseSecurityReceiver.class);
    }

    /**
     * Initiates the cascading provisioning strategy.
     * @return ProvisioningResult indicating the achieved privilege level.
     */
    public ProvisioningResult executeProvisioning() {
        // Check if already provisioned
        if (dpm.isDeviceOwnerApp(PACKAGE_NAME)) {
            Log.i(TAG, "Already Device Owner.");
            return ProvisioningResult.DEVICE_OWNER;
        }
        if (dpm.isProfileOwnerApp(PACKAGE_NAME)) {
            Log.i(TAG, "Already Profile Owner.");
            return ProvisioningResult.PROFILE_OWNER;
        }

        // Strategy A: Attempt root-assisted ADB-style provisioning
        if (attemptRootProvisioning()) {
            return ProvisioningResult.DEVICE_OWNER;
        }

        // Strategy B: Attempt Android Enterprise QR/NFC provisioning
        if (attemptEnterpriseProvisioning()) {
            return ProvisioningResult.PROVISIONING_INITIATED;
        }

        // Strategy C: Fallback to Work Profile (Profile Owner)
        if (attemptProfileOwnerProvisioning()) {
            return ProvisioningResult.PROFILE_OWNER;
        }

        Log.e(TAG, "All provisioning strategies failed.");
        return ProvisioningResult.FAILED;
    }

/**
       * Strategy A: Executes shell command with superuser privileges.
       * Requires rooted device or shell access (debug builds).
       */
    private boolean attemptRootProvisioning() {
        try {
            String command = String.format(
                "dpm set-device-owner %s/%s",
                PACKAGE_NAME, RECEIVER_NAME
            );

            Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", command});
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                Log.d(TAG, "Shell output: " + line);
            }

            int exitCode = process.exitValue();
            if (finished && exitCode == 0 && dpm.isAdminActive(adminComponent)) {
                Log.i(TAG, "Root provisioning succeeded. Admin active: " + dpm.isAdminActive(adminComponent));
                return true;
            }
            Log.w(TAG, "Root provisioning command executed but admin not active. Finished: " + finished + ", Exit code: " + exitCode);
        } catch (Exception e) {
            Log.w(TAG, "Root provisioning unavailable or failed: " + e.getMessage());
        }
        return false;
    }

/**
      * Strategy B: Launches Android Enterprise managed device provisioning intent.
      * Requires the device to be in initial setup state or factory-reset.
      */
    private boolean attemptEnterpriseProvisioning() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return false;
        }
        try {
            Intent intent = new Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_DEVICE);
            intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME, adminComponent);

            if (intent.resolveActivity(context.getPackageManager()) != null) {
                if (context instanceof Activity) {
                    ((Activity) context).startActivityForResult(intent, 1001);
                    Log.i(TAG, "Enterprise provisioning intent launched.");
                    return true;
                } else {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    Log.i(TAG, "Enterprise provisioning intent launched from non-activity context.");
                    Log.w(TAG, "Provisioning requires user interaction. Check device admin settings after completion.");
                    return false;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Enterprise provisioning failed: " + e.getMessage());
        }
        return false;
    }

/**
      * Strategy C: Creates a Work Profile as fallback when Device Owner is impossible.
      * Restricts camera and microphone within the managed profile.
      */
    private boolean attemptProfileOwnerProvisioning() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return false;
        }
        try {
            Intent intent = new Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE);
            intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME, adminComponent);

            if (intent.resolveActivity(context.getPackageManager()) != null) {
                if (context instanceof Activity) {
                    ((Activity) context).startActivityForResult(intent, 1002);
                    Log.i(TAG, "Profile Owner provisioning intent launched.");
                    return true;
                } else {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    Log.i(TAG, "Profile Owner provisioning intent launched from non-activity context.");
                    Log.w(TAG, "Provisioning requires user interaction. Check device admin settings after completion.");
                    return false;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Profile Owner provisioning failed: " + e.getMessage());
        }
        return false;
    }

    /**
     * Checks if the device has any existing accounts that would block Device Owner provisioning.
     */
    public boolean hasBlockingAccounts() {
        UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        // Note: Account detection requires READ_CONTACTS or similar; 
        // in production, attempt provisioning and catch the specific exception.
        return false; // Placeholder for actual account enumeration logic
    }

    public enum ProvisioningResult {
        DEVICE_OWNER,
        PROFILE_OWNER,
        PROVISIONING_INITIATED,
        FAILED
    }
}