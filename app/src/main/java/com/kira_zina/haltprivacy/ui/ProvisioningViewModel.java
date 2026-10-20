package com.kira_zina.haltprivacy.ui;

import android.app.Application;
import android.content.Intent;
import android.os.CountDownTimer;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.kira_zina.haltprivacy.AppIdentity;
import com.kira_zina.haltprivacy.provisioning.ProvisioningEngine;
import com.kira_zina.haltprivacy.security.HardwareControlEngine;
import com.kira_zina.haltprivacy.security.ManagedConfig;
import com.kira_zina.haltprivacy.security.ManagedConfigManager;
import com.kira_zina.haltprivacy.security.ManagedPolicyApplier;
import com.kira_zina.haltprivacy.security.PrefsManager;
import com.kira_zina.haltprivacy.security.TimedLockoutController;

import java.util.LinkedHashSet;
import java.util.Set;

public class ProvisioningViewModel extends AndroidViewModel {

    /**
     * Command published to the Activity when a lock-task (kiosk) session has to
     * be entered or left. The ViewModel never holds an Activity reference; the
     * Activity performs the session transition and reports the result back.
     */
    public enum LockTaskCommand { ENTER, EXIT }

    private final MutableLiveData<String> statusMessage = new MutableLiveData<>("Checking provisioning state...");
    private final MutableLiveData<Boolean> isLockdownActive;
    private final MutableLiveData<String> privilegeLevel = new MutableLiveData<>("None");
    private final MutableLiveData<Intent> provisioningIntent = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> isKioskModeConfigured;
    private final MutableLiveData<Boolean> isKioskModeActive = new MutableLiveData<>(false);
    private final MutableLiveData<Set<String>> allowedKioskPackages =
            new MutableLiveData<>(new LinkedHashSet<>());
    private final MutableLiveData<LockTaskCommand> lockTaskCommand = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> isTimedLockoutActive;
    private final MutableLiveData<String> remainingTimeFormatted = new MutableLiveData<>("");

    private final ProvisioningEngine provisioningEngine;
    private final HardwareControlEngine hardwareEngine;
    private final PrefsManager prefsManager;
    private final TimedLockoutController timedLockout;
    private final ManagedPolicyApplier managedPolicyApplier;
    private final String packageName;

    private CountDownTimer countDownTimer;

    public ProvisioningViewModel(@NonNull Application application) {
        this(application,
                new ProvisioningEngine(application),
                new HardwareControlEngine(application),
                new PrefsManager(application));
    }

    @VisibleForTesting
    ProvisioningViewModel(@NonNull Application application,
                          ProvisioningEngine provisioningEngine,
                          HardwareControlEngine hardwareEngine,
                          PrefsManager prefsManager) {
        super(application);
        this.provisioningEngine = provisioningEngine;
        this.hardwareEngine = hardwareEngine;
        this.prefsManager = prefsManager;
        this.timedLockout = new TimedLockoutController(prefsManager);
        this.managedPolicyApplier = new ManagedPolicyApplier(hardwareEngine, prefsManager);
        this.packageName = AppIdentity.packageName(application);

        this.isLockdownActive = new MutableLiveData<>(prefsManager.isLockdownActive());
        this.isKioskModeConfigured = new MutableLiveData<>(prefsManager.isKioskModeConfigured());
        this.isTimedLockoutActive = new MutableLiveData<>(prefsManager.isTimedLockoutActive());
        refreshStatus();

        if (Boolean.TRUE.equals(isTimedLockoutActive.getValue())) {
            startCountdown(timedLockout.endTimestamp());
        }
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
        if (isTimedLockoutBlocked()) {
            return;
        }
        hardwareEngine.disableHardwarePrivacy();
        refreshStatus();
    }

    /**
     * Starts a strict timed lockout: the privacy and kiosk policies are applied
     * and the deadline is persisted, so it survives a reboot. While the
     * deadline lies in the future the policy cannot be disabled.
     *
     * @param durationMillis how long the lockout should last
     */
    public void startTimedLockout(long durationMillis) {
        if (durationMillis <= 0L) {
            statusMessage.postValue("Invalid lockout duration.");
            return;
        }

        if (!hardwareEngine.isDeviceOwner()) {
            statusMessage.postValue("Timed lockout requires Device Owner mode.");
            return;
        }

        long endTimestamp = timedLockout.start(durationMillis, System.currentTimeMillis());

        hardwareEngine.enableHardwarePrivacy();
        hardwareEngine.enableTamperProofing(currentAllowedKioskPackages());
        prefsManager.setKioskModeConfigured(true);
        isKioskModeConfigured.postValue(true);

        isTimedLockoutActive.postValue(true);
        remainingTimeFormatted.postValue(TimedLockoutController.formatRemaining(durationMillis));
        startCountdown(endTimestamp);

        statusMessage.postValue("Timed lockout started. Policy cannot be disabled until the timer expires.");
        lockTaskCommand.postValue(LockTaskCommand.ENTER);
    }

    /**
     * Reads the managed configuration that the device management console pushed
     * to this application and enforces it.
     */
    public void applyManagedConfigurations() {
        applyManagedConfigurations(new ManagedConfigManager(getApplication()).readManagedConfig());
    }

    /**
     * Enforces a managed configuration.
     *
     * <p>The remote configuration uses the same policy mechanisms as the
     * operator console: hardware privacy, kiosk mode with its allowlist and the
     * timed lockout.</p>
     *
     * @param config the parsed configuration; may be null
     */
    public void applyManagedConfigurations(ManagedConfig config) {
        if (config == null) {
            return;
        }

        String summary = managedPolicyApplier.apply(config);
        refreshStatus();

        if (Boolean.TRUE.equals(isTimedLockoutActive.getValue())) {
            startCountdown(timedLockout.endTimestamp());
        }

        if (!config.isEmpty()) {
            statusMessage.postValue(summary);
        }
    }

    /**
     * @return the managed configuration that is currently in force
     */
    public ManagedConfig readManagedConfig() {
        return new ManagedConfigManager(getApplication()).readManagedConfig();
    }

    /**
     * Checks whether a timed lockout currently prevents the policy from being
     * disabled. When it does, the operator is told how long is left.
     *
     * @return true when the disable action must be refused
     */
    public boolean isTimedLockoutBlocked() {
        if (!Boolean.TRUE.equals(isTimedLockoutActive.getValue())) {
            return false;
        }

        long remaining = timedLockout.remainingMillis();
        statusMessage.postValue("Cannot disable: Timed lockout active for remaining "
                + TimedLockoutController.remainingMinutes(remaining) + " min");
        return true;
    }

    /**
     * Starts the one second countdown ticker that publishes the remaining time.
     */
    private void startCountdown(long endTimestamp) {
        cancelCountdown();

        long remaining = endTimestamp - System.currentTimeMillis();
        if (remaining <= 0L) {
            onTimedLockoutExpired();
            return;
        }

        countDownTimer = new CountDownTimer(remaining, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingTimeFormatted.postValue(
                        TimedLockoutController.formatRemaining(millisUntilFinished));
            }

            @Override
            public void onFinish() {
                onTimedLockoutExpired();
            }
        };
        countDownTimer.start();
    }

    private void cancelCountdown() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    /**
     * The deadline has passed: the lockout guard is lifted, while the policy
     * itself stays in place until the operator removes it.
     */
    private void onTimedLockoutExpired() {
        cancelCountdown();
        timedLockout.clear();
        isTimedLockoutActive.postValue(false);
        remainingTimeFormatted.postValue("");
        statusMessage.postValue("Timed lockout expired. Lockdown can now be disabled.");
    }

    /**
     * Configures the device for kiosk operation and asks the Activity to enter
     * a lock-task session.
     *
     * <p>The Activity performs {@code startLockTask()} — only the foreground
     * activity can enter lock-task mode — and reports the resulting state back
     * through {@link #onLockTaskCommandHandled(boolean)}.</p>
     */
    public void enableKioskMode() {
        if (!hardwareEngine.isDeviceOwner()) {
            statusMessage.postValue("Kiosk mode requires Device Owner mode.");
            return;
        }

        Set<String> packages = currentAllowedKioskPackages();
        if (!hardwareEngine.enableTamperProofing(packages)) {
            statusMessage.postValue("Failed to enable kiosk mode.");
            return;
        }

        prefsManager.setKioskModeConfigured(true);
        isKioskModeConfigured.postValue(true);
        statusMessage.postValue("Kiosk mode enabled. Entering lock task mode...");
        lockTaskCommand.postValue(LockTaskCommand.ENTER);
    }

    /**
     * Clears the kiosk configuration and asks the Activity to leave the
     * lock-task session. Refused while a timed lockout is running.
     */
    public void disableKioskMode() {
        if (isTimedLockoutBlocked()) {
            return;
        }

        if (!hardwareEngine.isDeviceOwner()) {
            statusMessage.postValue("Kiosk mode requires Device Owner mode.");
            return;
        }

        if (!hardwareEngine.disableTamperProofing()) {
            statusMessage.postValue("Failed to disable kiosk mode.");
            return;
        }

        prefsManager.setKioskModeConfigured(false);
        isKioskModeConfigured.postValue(false);
        statusMessage.postValue("Kiosk mode disabled. Leaving lock task mode...");
        lockTaskCommand.postValue(LockTaskCommand.EXIT);
    }

    /**
     * Called by the Activity once it has handled a {@link LockTaskCommand}.
     *
     * @param inLockTaskMode true when a lock-task session is now active
     */
    public void onLockTaskCommandHandled(boolean inLockTaskMode) {
        updateKioskState(inLockTaskMode);
        lockTaskCommand.postValue(null);
    }

    /**
     * Reports the live lock-task state of the device. Values that did not
     * change are ignored so the status indicator is not re-animated on every
     * window focus change.
     */
    public void updateKioskState(boolean active) {
        Boolean current = isKioskModeActive.getValue();
        if (current != null && current == active) {
            return;
        }
        isKioskModeActive.postValue(active);
    }

    /**
     * @return the secondary applications the operator allowed to run inside a
     *         kiosk session; never null
     */
    public Set<String> currentAllowedKioskPackages() {
        Set<String> current = allowedKioskPackages.getValue();
        if (current == null) {
            return new LinkedHashSet<>();
        }
        return new LinkedHashSet<>(current);
    }

    /**
     * Adds a secondary application to the kiosk allowlist.
     *
     * <p>Invalid package names and duplicates are ignored. When kiosk mode is
     * already active the lock-task configuration is re-applied immediately so
     * the change takes effect without leaving the session.</p>
     *
     * @param packageName the package to allow
     * @return true when the package was added
     */
    public boolean addAllowedPackage(String packageName) {
        if (!AppIdentity.isValidPackageName(packageName)) {
            statusMessage.postValue("Invalid package name: " + packageName);
            return false;
        }

        String normalized = packageName.trim();
        if (normalized.equals(this.packageName)) {
            statusMessage.postValue("This application is always allowed in kiosk mode.");
            return false;
        }

        Set<String> current = currentAllowedKioskPackages();
        if (current.contains(normalized)) {
            statusMessage.postValue(normalized + " is already in the kiosk allowlist.");
            return false;
        }

        current.add(normalized);
        persistAllowedKioskPackages(current);
        statusMessage.postValue("Added " + normalized + " to the kiosk allowlist.");
        return true;
    }

    /**
     * Removes a secondary application from the kiosk allowlist.
     *
     * @param packageName the package to remove
     * @return true when the package was removed
     */
    public boolean removeAllowedPackage(String packageName) {
        if (packageName == null) {
            return false;
        }

        Set<String> current = currentAllowedKioskPackages();
        if (!current.remove(packageName)) {
            return false;
        }

        persistAllowedKioskPackages(current);
        statusMessage.postValue("Removed " + packageName + " from the kiosk allowlist.");
        return true;
    }

    /**
     * Persists the allowlist, publishes it and re-applies the lock-task
     * configuration when a kiosk session is already running.
     */
    private void persistAllowedKioskPackages(Set<String> packages) {
        Set<String> copy = new LinkedHashSet<>(packages);
        prefsManager.setAllowedKioskPackages(copy);
        allowedKioskPackages.postValue(copy);

        if (hardwareEngine.isKioskModeConfigured()) {
            hardwareEngine.enableKioskMode(copy);
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

            boolean kioskConfigured = hardwareEngine.isKioskModeConfigured();
            isKioskModeConfigured.postValue(kioskConfigured);
            prefsManager.setKioskModeConfigured(kioskConfigured);
            if (!kioskConfigured) {
                isKioskModeActive.postValue(false);
            }

            // The operator intent (persisted) and the live device policy state
            // are merged so that neither an external reset nor a pending
            // selection is lost.
            Set<String> persistedPackages = prefsManager.getAllowedKioskPackages();
            Set<String> merged = persistedPackages == null
                    ? new LinkedHashSet<>()
                    : new LinkedHashSet<>(persistedPackages);
            Set<String> livePackages = hardwareEngine.getAllowedKioskPackages();
            if (livePackages != null) {
                for (String live : livePackages) {
                    if (!live.equals(packageName)) {
                        merged.add(live);
                    }
                }
            }
            prefsManager.setAllowedKioskPackages(merged);
            allowedKioskPackages.postValue(merged);

            // A timed lockout that has expired no longer blocks anything.
            boolean timedLockoutActive = prefsManager.isTimedLockoutActive();
            if (!timedLockoutActive && timedLockout.endTimestamp() > 0L) {
                timedLockout.clear();
            }
            isTimedLockoutActive.postValue(timedLockoutActive);
            if (!timedLockoutActive) {
                remainingTimeFormatted.postValue("");
            } else {
                remainingTimeFormatted.postValue(
                        TimedLockoutController.formatRemaining(timedLockout.remainingMillis()));
            }

        } catch (Exception e) {
            privilegeLevel.postValue("Unknown");
            isLockdownActive.postValue(false);
            isKioskModeConfigured.postValue(false);
            isKioskModeActive.postValue(false);
        }
    }

    @Override
    protected void onCleared() {
        cancelCountdown();
        super.onCleared();
    }

    public LiveData<String> getStatusMessage() { return statusMessage; }
    public LiveData<Boolean> getIsLockdownActive() { return isLockdownActive; }
    public LiveData<String> getPrivilegeLevel() { return privilegeLevel; }
    public LiveData<Intent> getProvisioningIntent() { return provisioningIntent; }
    public LiveData<Boolean> getIsKioskModeConfigured() { return isKioskModeConfigured; }
    public LiveData<Boolean> getIsKioskModeActive() { return isKioskModeActive; }
    public LiveData<Set<String>> getAllowedKioskPackages() { return allowedKioskPackages; }
    public LiveData<LockTaskCommand> getLockTaskCommand() { return lockTaskCommand; }
    public LiveData<Boolean> getIsTimedLockoutActive() { return isTimedLockoutActive; }
    public LiveData<String> getRemainingTimeFormatted() { return remainingTimeFormatted; }
}
