package com.kira_zina.haltprivacy.security;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Applies an Android Enterprise managed configuration to the device.
 *
 * <p>The remote configuration is the counterpart of the operator console: the
 * same policy mechanisms are used, but the decisions come from the device
 * management console instead of the user.</p>
 */
public class ManagedPolicyApplier {
    private static final String TAG = "ManagedPolicyApplier";

    private final HardwareControlEngine hardwareEngine;
    private final PrefsManager prefsManager;
    private final TimedLockoutController timedLockout;

    public ManagedPolicyApplier(Context context) {
        this(new HardwareControlEngine(context), new PrefsManager(context));
    }

    public ManagedPolicyApplier(HardwareControlEngine hardwareEngine, PrefsManager prefsManager) {
        this.hardwareEngine = hardwareEngine;
        this.prefsManager = prefsManager;
        this.timedLockout = new TimedLockoutController(prefsManager);
    }

    /**
     * Enforces a managed configuration.
     *
     * @param config the parsed configuration; may be null
     * @return a human readable summary of what was enforced
     */
    public String apply(ManagedConfig config) {
        if (config == null) {
            return "Managed configuration: none";
        }

        List<String> applied = new ArrayList<>();

        if (config.isHardwarePrivacyEnabled()) {
            hardwareEngine.enableHardwarePrivacy();
            applied.add("hardware privacy");
        }

        Set<String> allowedPackages = config.getAllowedPackages();
        boolean kioskRequested = config.isKioskModeEnabled() || config.getTimedLockoutMinutes() > 0;

        if (kioskRequested) {
            prefsManager.setAllowedKioskPackages(allowedPackages);
            if (hardwareEngine.enableTamperProofing(allowedPackages)) {
                prefsManager.setKioskModeConfigured(true);
                applied.add("kiosk mode (" + allowedPackages.size() + " allowed apps)");
            } else {
                Log.w(TAG, "Managed configuration requests kiosk mode, but it was not applied.");
            }
        }

        if (config.getTimedLockoutMinutes() > 0) {
            if (!timedLockout.isActive()) {
                timedLockout.start(config.getTimedLockoutMinutes() * 60_000L,
                        System.currentTimeMillis());
            }
            applied.add("timed lockout " + config.getTimedLockoutMinutes() + " min");
        }

        if (applied.isEmpty()) {
            return "Managed configuration: no action required";
        }
        return "Managed configuration applied: " + String.join(", ", applied);
    }
}
