package com.kira_zina.haltprivacy.security;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefsManager {
    private static final String PREF_NAME = "halt_privacy_prefs";
    private static final String KEY_LOCKDOWN_ACTIVE = "lockdown_active";
    private static final String KEY_PRIVILEGE_LEVEL = "privilege_level";

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
}
