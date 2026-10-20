package com.kira_zina.haltprivacy.security;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Parsed representation of the Android Enterprise managed configuration that
 * the device management console pushed to this application.
 *
 * <p>Every field has a safe default, so an absent, empty or malformed
 * configuration simply means "nothing is enforced remotely".</p>
 */
public class ManagedConfig {

    private boolean hardwarePrivacyEnabled;
    private boolean kioskModeEnabled;
    private int timedLockoutMinutes;
    private final Set<String> allowedPackages = new LinkedHashSet<>();

    public boolean isHardwarePrivacyEnabled() {
        return hardwarePrivacyEnabled;
    }

    public void setHardwarePrivacyEnabled(boolean hardwarePrivacyEnabled) {
        this.hardwarePrivacyEnabled = hardwarePrivacyEnabled;
    }

    public boolean isKioskModeEnabled() {
        return kioskModeEnabled;
    }

    public void setKioskModeEnabled(boolean kioskModeEnabled) {
        this.kioskModeEnabled = kioskModeEnabled;
    }

    /**
     * @return the secondary applications the EMM allows inside the kiosk
     *         session; never null
     */
    public Set<String> getAllowedPackages() {
        return Collections.unmodifiableSet(allowedPackages);
    }

    public void setAllowedPackages(Collection<String> packages) {
        allowedPackages.clear();
        if (packages != null) {
            for (String candidate : packages) {
                if (candidate != null && !candidate.trim().isEmpty()) {
                    allowedPackages.add(candidate.trim());
                }
            }
        }
    }

    /**
     * @return the duration of the remotely requested timed lockout in minutes;
     *         0 means that no timed lockout was requested
     */
    public int getTimedLockoutMinutes() {
        return timedLockoutMinutes;
    }

    public void setTimedLockoutMinutes(int timedLockoutMinutes) {
        this.timedLockoutMinutes = Math.max(0, timedLockoutMinutes);
    }

    /**
     * @return true when the configuration asks for at least one action
     */
    public boolean isEmpty() {
        return !hardwarePrivacyEnabled
                && !kioskModeEnabled
                && timedLockoutMinutes <= 0
                && allowedPackages.isEmpty();
    }

    @Override
    public String toString() {
        return "ManagedConfig{hardwarePrivacy=" + hardwarePrivacyEnabled
                + ", kioskMode=" + kioskModeEnabled
                + ", timedLockoutMinutes=" + timedLockoutMinutes
                + ", allowedPackages=" + allowedPackages
                + '}';
    }
}
