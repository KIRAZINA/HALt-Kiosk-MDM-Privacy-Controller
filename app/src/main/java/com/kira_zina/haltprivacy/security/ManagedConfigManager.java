package com.kira_zina.haltprivacy.security;

import android.content.Context;
import android.content.RestrictionsManager;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Reads the Android Enterprise managed configuration and turns it into a
 * {@link ManagedConfig}.
 *
 * <p>The keys correspond to the schema declared in
 * {@code res/xml/app_restrictions.xml}.</p>
 */
public class ManagedConfigManager {
    private static final String TAG = "ManagedConfigManager";

    /** Enables the camera and microphone restrictions remotely. */
    public static final String KEY_HARDWARE_PRIVACY_ENABLED = "hardware_privacy_enabled";

    /** Enables the lock task (kiosk) mode remotely. */
    public static final String KEY_KIOSK_MODE_ENABLED = "kiosk_mode_enabled";

    /** Secondary applications allowed to run inside the kiosk session. */
    public static final String KEY_KIOSK_ALLOWED_PACKAGES = "kiosk_allowed_packages";

    /** Duration of the remotely requested timed lockout, in minutes. */
    public static final String KEY_TIMED_LOCKOUT_DURATION_MINUTES = "timed_lockout_duration_minutes";

    private final Context context;

    public ManagedConfigManager(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Reads the configuration that the device management console pushed to this
     * application.
     *
     * @return the parsed configuration; never null
     */
    public ManagedConfig readManagedConfig() {
        return parse(readManagedRestrictions());
    }

    /**
     * @return the raw restrictions bundle, or null when none are available
     */
    public Bundle readManagedRestrictions() {
        try {
            RestrictionsManager restrictionsManager =
                    (RestrictionsManager) context.getSystemService(Context.RESTRICTIONS_SERVICE);
            if (restrictionsManager == null) {
                Log.w(TAG, "RestrictionsManager is not available on this device.");
                return null;
            }
            return restrictionsManager.getApplicationRestrictions();
        } catch (Exception e) {
            Log.w(TAG, "Failed to read managed restrictions: " + e.getMessage());
            return null;
        }
    }

    /**
     * Parses a restrictions bundle into a {@link ManagedConfig}. Missing keys,
     * null values and values of an unexpected type fall back to the default,
     * so a malformed configuration can never crash the parser.
     *
     * @param bundle the raw restrictions; may be null
     * @return the parsed configuration; never null
     */
    public static ManagedConfig parse(Bundle bundle) {
        ManagedConfig config = new ManagedConfig();
        if (bundle == null) {
            return config;
        }

        config.setHardwarePrivacyEnabled(readBoolean(bundle, KEY_HARDWARE_PRIVACY_ENABLED));
        config.setKioskModeEnabled(readBoolean(bundle, KEY_KIOSK_MODE_ENABLED));
        config.setAllowedPackages(readStringSet(bundle, KEY_KIOSK_ALLOWED_PACKAGES));
        config.setTimedLockoutMinutes(readInt(bundle, KEY_TIMED_LOCKOUT_DURATION_MINUTES));
        return config;
    }

    private static boolean readBoolean(Bundle bundle, String key) {
        try {
            return bundle.getBoolean(key, false);
        } catch (Exception e) {
            Log.w(TAG, "Managed config key '" + key + "' is not a boolean; using the default.");
            return false;
        }
    }

    private static int readInt(Bundle bundle, String key) {
        try {
            return bundle.getInt(key, 0);
        } catch (Exception e) {
            Log.w(TAG, "Managed config key '" + key + "' is not an integer; using the default.");
            return 0;
        }
    }

    /**
     * Reads the allowlist restriction. The management console delivers the
     * selected entries as a string array, but some consoles push a single
     * comma separated string instead — both are accepted.
     */
    private static Set<String> readStringSet(Bundle bundle, String key) {
        Set<String> values = new LinkedHashSet<>();

        try {
            String[] array = bundle.getStringArray(key);
            if (array != null) {
                for (String value : array) {
                    addIfUsable(values, value);
                }
                return values;
            }
        } catch (Exception e) {
            Log.w(TAG, "Managed config key '" + key + "' is not a string array; trying a list.");
        }

        try {
            ArrayList<String> list = bundle.getStringArrayList(key);
            if (list != null) {
                for (String value : list) {
                    addIfUsable(values, value);
                }
                return values;
            }
        } catch (Exception e) {
            Log.w(TAG, "Managed config key '" + key + "' is not a string list; trying a string.");
        }

        try {
            String single = bundle.getString(key);
            if (single != null) {
                for (String part : single.split("[,;\\s]+")) {
                    addIfUsable(values, part);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Managed config key '" + key + "' has an unsupported type; using the default.");
        }

        return values;
    }

    /**
     * Also accepts a plain collection, which some management consoles use.
     */
    private static void addIfUsable(Set<String> target, String value) {
        if (value != null && !value.trim().isEmpty()) {
            target.add(value.trim());
        }
    }


}