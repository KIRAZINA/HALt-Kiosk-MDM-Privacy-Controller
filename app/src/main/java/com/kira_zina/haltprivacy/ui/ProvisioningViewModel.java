package com.kira_zina.haltprivacy.ui;

import android.app.Application;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.kira_zina.haltprivacy.provisioning.ProvisioningEngine;
import com.kira_zina.haltprivacy.security.HardwareControlEngine;
import com.kira_zina.haltprivacy.security.PrefsManager;

public class ProvisioningViewModel extends AndroidViewModel {
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>("Checking provisioning state...");
    private final MutableLiveData<Boolean> isLockdownActive;
    private final MutableLiveData<String> privilegeLevel = new MutableLiveData<>("None");
    private final MutableLiveData<Intent> provisioningIntent = new MutableLiveData<>(null);

    private final ProvisioningEngine provisioningEngine;
    private final HardwareControlEngine hardwareEngine;
    private final PrefsManager prefsManager;

    public ProvisioningViewModel(@NonNull Application application) {
        super(application);
        this.provisioningEngine = new ProvisioningEngine(application);
        this.hardwareEngine = new HardwareControlEngine(application);
        this.prefsManager = new PrefsManager(application);

        this.isLockdownActive = new MutableLiveData<>(prefsManager.isLockdownActive());
        refreshStatus();
    }

    public void attemptAutoProvisioning() {
        ProvisioningEngine.ProvisioningResult result = provisioningEngine.executeProvisioning();

        switch (result) {
            case DEVICE_OWNER:
                statusMessage.postValue("Already Device Owner.");
                refreshStatus();
                return;
            case PROFILE_OWNER:
                statusMessage.postValue("Already Profile Owner.");
                refreshStatus();
                return;
        }

        // Not provisioned — try enterprise provisioning first
        Intent intent = provisioningEngine.getEnterpriseProvisioningIntent();
        if (intent != null) {
            statusMessage.postValue("Launching enterprise provisioning...");
            provisioningIntent.postValue(intent);
            return;
        }

        // Fall back to profile owner
        intent = provisioningEngine.getProfileOwnerProvisioningIntent();
        if (intent != null) {
            statusMessage.postValue("Launching profile provisioning...");
            provisioningIntent.postValue(intent);
            return;
        }

        statusMessage.postValue("Failed: No provisioning method available.");
    }

    public void onProvisioningResult(boolean success) {
        if (success) {
            statusMessage.postValue("Provisioning succeeded. Refreshing status...");
        } else {
            statusMessage.postValue("Provisioning was cancelled or failed.");
        }
        provisioningIntent.postValue(null);
        refreshStatus();
    }

    public void enableLockdown() {
        hardwareEngine.enableHardwarePrivacy();
        refreshStatus();
    }

    public void disableLockdown() {
        hardwareEngine.disableHardwarePrivacy();
        refreshStatus();
    }

    public void enableTamperProofing() {
        if (hardwareEngine.isDeviceOwner()) {
            hardwareEngine.enableTamperProofing();
            statusMessage.postValue("Tamper-proofing enabled.");
        } else {
            statusMessage.postValue("Tamper-proofing requires Device Owner mode.");
        }
    }

    public void disableTamperProofing() {
        if (hardwareEngine.isDeviceOwner()) {
            hardwareEngine.disableTamperProofing();
            statusMessage.postValue("Tamper-proofing disabled.");
        }
    }

    public boolean isDeviceOwner() {
        return hardwareEngine.isDeviceOwner();
    }

    public void refreshStatus() {
        try {
            String level;
            if (hardwareEngine.isDeviceOwner()) {
                level = "Device Owner";
            } else if (hardwareEngine.isProfileOwner()) {
                level = "Profile Owner";
            } else {
                level = "None";
            }
            privilegeLevel.postValue(level);
            prefsManager.setPrivilegeLevel(level);

            boolean lockdownActive = false;
            try {
                lockdownActive = hardwareEngine.isPrivacyModeEnforced();
            } catch (Exception e) {
                lockdownActive = false;
            }
            isLockdownActive.postValue(lockdownActive);
            prefsManager.setLockdownActive(lockdownActive);

        } catch (Exception e) {
            privilegeLevel.postValue("Unknown");
            isLockdownActive.postValue(false);
        }
    }

    public LiveData<String> getStatusMessage() { return statusMessage; }
    public LiveData<Boolean> getIsLockdownActive() { return isLockdownActive; }
    public LiveData<String> getPrivilegeLevel() { return privilegeLevel; }
    public LiveData<Intent> getProvisioningIntent() { return provisioningIntent; }
}
