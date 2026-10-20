package com.kira_zina.haltprivacy;

import android.content.Context;

import java.util.regex.Pattern;

/**
 * Single source of truth for the application's own package name.
 *
 * <p>Device policy role checks (Device Owner / Profile Owner) and the
 * uninstall-block target are keyed on the application id, so the value must
 * stay in sync with {@code applicationId} in the Gradle build script. Resolving
 * it at runtime from the owning {@link Context} keeps it correct across
 * application id changes, with {@link BuildConfig#APPLICATION_ID} as the
 * compile-time fallback.</p>
 *
 * <p>The class also owns the validation rules for package names, so that
 * operator supplied values — for example the secondary applications allowed to
 * run inside a kiosk session — are checked against one definition.</p>
 */
public final class AppIdentity {

    /**
     * An Android package name: at least two dot separated segments, each
     * starting with a letter or underscore and continuing with letters,
     * digits or underscores.
     */
    private static final Pattern PACKAGE_NAME_PATTERN =
            Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)+$");

    /** Android limits package names to 255 characters. */
    private static final int MAX_PACKAGE_NAME_LENGTH = 255;

    private AppIdentity() {
    }

    /**
     * @param context context used to resolve the runtime package name; may be null
     * @return the application id, never null or empty
     */
    public static String packageName(Context context) {
        if (context != null) {
            String runtimeName = context.getPackageName();
            if (runtimeName != null && !runtimeName.isEmpty()) {
                return runtimeName;
            }
        }
        return BuildConfig.APPLICATION_ID;
    }

    /**
     * Validates an operator supplied package name.
     *
     * @param packageName the candidate value; may be null
     * @return true when the value is a syntactically valid Android package name
     */
    public static boolean isValidPackageName(String packageName) {
        if (packageName == null) {
            return false;
        }

        String trimmed = packageName.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_PACKAGE_NAME_LENGTH) {
            return false;
        }

        return PACKAGE_NAME_PATTERN.matcher(trimmed).matches();
    }
}
