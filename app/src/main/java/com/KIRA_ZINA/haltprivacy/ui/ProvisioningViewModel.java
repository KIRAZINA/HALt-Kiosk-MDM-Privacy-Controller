package com.KIRA_ZINA.haltprivacy.ui;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.KIRA_ZINA.haltprivacy.provisioning.ProvisioningEngine;
import com.KIRA_ZINA.haltprivacy.security.HardwareControlEngine;

/**
 * ViewModel orchestrating provisioning attempts and hardware restriction state.
 */
public class ProvisioningViewModel extends AndroidViewModel {
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>("Checking provisioning state...");
    private final MutableLiveData<Boolean> isLockdownActive = new MutableLiveData<>(false);
    private final MutableLiveData<String> privilegeLevel = new MutableLiveData<>("None");

    private final ProvisioningEngine provisioningEngine;
    private final HardwareControlEngine hardwareEngine;

    public ProvisioningViewModel(@NonNull Application application) {
        super(application);
        this.provisioningEngine = new ProvisioningEngine(application);
        this.hardwareEngine = new HardwareControlEngine(application);
        refreshStatus();
    }

    public void attemptAutoProvisioning() {
        ProvisioningEngine.ProvisioningResult result = provisioningEngine.executeProvisioning();
        switch (result) {
            case DEVICE_OWNER:
                statusMessage.postValue("Success: Elevated to Device Owner.");
                break;
            case PROFILE_OWNER:
                statusMessage.postValue("Fallback: Work Profile created (Profile Owner).");
                break;
            case PROVISIONING_INITIATED:
                statusMessage.postValue("Provisioning initiated. Follow system prompts to complete.");
                break;
            case FAILED:
                statusMessage.postValue("Failed: No rooted device, factory reset required, or existing accounts block provisioning.");
                break;
        }
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
            if (hardwareEngine.isDeviceOwner()) {
                privilegeLevel.postValue("Device Owner");
            } else if (hardwareEngine.isProfileOwner()) {
                privilegeLevel.postValue("Profile Owner");
            } else {
                privilegeLevel.postValue("None");
            }

            boolean lockdownActive = false;
            try {
                lockdownActive = hardwareEngine.isPrivacyModeEnforced();
            } catch (Exception e) {
                // Safely fallback if any error occurs during privacy check
                lockdownActive = false;
            }
            isLockdownActive.postValue(lockdownActive);
        } catch (Exception e) {
            // If refreshStatus fails entirely, set safe defaults
            privilegeLevel.postValue("Unknown");
            isLockdownActive.postValue(false);
        }
    }

    public LiveData<String> getStatusMessage() { return statusMessage; }
    public LiveData<Boolean> getIsLockdownActive() { return isLockdownActive; }
    public LiveData<String> getPrivilegeLevel() { return privilegeLevel; }
}