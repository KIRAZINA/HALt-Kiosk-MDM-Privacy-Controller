package com.kira_zina.haltprivacy.provisioning;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import com.kira_zina.haltprivacy.AppIdentity;
import com.kira_zina.haltprivacy.security.EnterpriseSecurityReceiver;

public class ProvisioningEngine {
    private static final String TAG = "ProvisioningEngine";

    private final DevicePolicyManager dpm;
    private final ComponentName adminComponent;
    private final Context context;
    private final String packageName;

    public ProvisioningEngine(Context context) {
        this.context = context.getApplicationContext();
        this.dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        this.adminComponent = new ComponentName(context, EnterpriseSecurityReceiver.class);
        this.packageName = AppIdentity.packageName(this.context);
    }

    public ProvisioningResult executeProvisioning() {
        if (dpm.isDeviceOwnerApp(packageName)) {
            Log.i(TAG, "Already Device Owner.");
            return ProvisioningResult.DEVICE_OWNER;
        }
        if (dpm.isProfileOwnerApp(packageName)) {
            Log.i(TAG, "Already Profile Owner.");
            return ProvisioningResult.PROFILE_OWNER;
        }

        if (hasBlockingAccounts()) {
            Log.w(TAG, "Google accounts detected. Device Owner provisioning may fail.");
        }

        return ProvisioningResult.FAILED;
    }

    public Intent getEnterpriseProvisioningIntent() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return null;
        try {
            Intent intent = new Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_DEVICE);
            intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME, adminComponent);
            if (canResolveIntent(intent)) {
                Log.i(TAG, "Enterprise provisioning intent created.");
                return intent;
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to create enterprise provisioning intent: " + e.getMessage());
        }
        return null;
    }

    public Intent getProfileOwnerProvisioningIntent() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return null;
        try {
            Intent intent = new Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE);
            intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME, adminComponent);
            if (canResolveIntent(intent)) {
                Log.i(TAG, "Profile Owner provisioning intent created.");
                return intent;
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to create profile owner provisioning intent: " + e.getMessage());
        }
        return null;
    }

    private boolean canResolveIntent(Intent intent) {
        PackageManager pm = context.getPackageManager();
        if (pm == null) return false;
        try {
            return pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null;
        } catch (Exception e) {
            Log.w(TAG, "Resolution check failed: " + e.getMessage());
            return true;
        }
    }

    @SuppressWarnings("deprecation")
    public boolean hasBlockingAccounts() {
        try {
            AccountManager accountManager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
            if (accountManager == null) {
                Log.w(TAG, "AccountManager not available.");
                return false;
            }
            Account[] accounts = accountManager.getAccountsByType("com.google");
            if (accounts != null && accounts.length > 0) {
                for (Account account : accounts) {
                    Log.w(TAG, "Blocking account found: type=" + account.type + " name=" + account.name);
                }
                return true;
            }
        } catch (SecurityException e) {
            Log.w(TAG, "Cannot check accounts: " + e.getMessage());
        }
        return false;
    }

    public enum ProvisioningResult {
        DEVICE_OWNER,
        PROFILE_OWNER,
        PROVISIONING_INITIATED,
        FAILED
    }
}
