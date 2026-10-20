package com.kira_zina.haltprivacy.security;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.LinkedHashSet;
import java.util.Set;

public class PrefsManager {
    private static final String PREF_NAME = "halt_privacy_prefs";
    private static final String KEY_LOCKDOWN_ACTIVE = "lockdown_active";
    private static final String KEY_PRIVILEGE_LEVEL = "privilege_level";
    private static final String KEY_KIOSK_MODE_CONFIGURED = "kiosk_mode_configured";
    private static final String KEY_ALLOWED_KIOSK_PACKAGES = "allowed_kiosk_packages";
    private static final String KEY_LOCKOUT_END_TIMESTAMP = "lockout_end_timestamp_ms";

    private final SharedPreferences prefs;

    public PrefsManager(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean isLockdownActive() {
        return prefs.getBoolean(KEY_LOCKDOWN_ACTIVE, false);
    }

    public void setLockdownActive(boolean active) {
        prefs.edit().putBoolean(KEY_LOCKDOWN_ACTIVE, active).apply();
    }

    public String getPrivilegeLevel() {
        return prefs.getString(KEY_PRIVILEGE_LEVEL, "None");
    }

    public void setPrivilegeLevel(String level) {
        prefs.edit().putString(KEY_PRIVILEGE_LEVEL, level).apply();
    }

    /**
     * @return true when this package is currently registered as a lock-task
     *         (kiosk) package on the device
     */
    public boolean isKioskModeConfigured() {
        return prefs.getBoolean(KEY_KIOSK_MODE_CONFIGURED, false);
    }

    public void setKioskModeConfigured(boolean configured) {
        prefs.edit().putBoolean(KEY_KIOSK_MODE_CONFIGURED, configured).apply();
    }

    /**
     * @return the secondary applications the operator allowed to run inside a
     *         kiosk session; never null
     */
    public Set<String> getAllowedKioskPackages() {
        Set<String> stored = prefs.getStringSet(KEY_ALLOWED_KIOSK_PACKAGES, null);
        if (stored == null || stored.isEmpty()) {
            return new LinkedHashSet<>();
        }
        return new LinkedHashSet<>(stored);
    }

    public void setAllowedKioskPackages(Set<String> packages) {
        Set<String> copy = new LinkedHashSet<>();
        if (packages != null) {
            copy.addAll(packages);
        }
        prefs.edit().putStringSet(KEY_ALLOWED_KIOSK_PACKAGES, copy).apply();
    }

    /**
     * @return the wall-clock time (epoch milliseconds) at which the current
     *         timed lockout expires, or 0 when no timed lockout is configured
     */
    public long getLockoutEndTimestamp() {
        return prefs.getLong(KEY_LOCKOUT_END_TIMESTAMP, 0L);
    }

    public void setLockoutEndTimestamp(long endTimestampMs) {
        prefs.edit().putLong(KEY_LOCKOUT_END_TIMESTAMP, endTimestampMs).apply();
    }

    public void clearLockoutEndTimestamp() {
        prefs.edit().remove(KEY_LOCKOUT_END_TIMESTAMP).apply();
    }

    /**
     * @return true while the stored end timestamp lies in the future
     */
    public boolean isTimedLockoutActive() {
        long endTimestamp = getLockoutEndTimestamp();
        return endTimestamp > 0L && System.currentTimeMillis() < endTimestamp;
    }
}
