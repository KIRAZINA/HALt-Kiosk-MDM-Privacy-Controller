package com.kira_zina.haltprivacy;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the package-name rules used by the kiosk allowlist.
 */
public class AppIdentityTest {

    @Test
    public void isValidPackageName_acceptsRealWorldPackageNames() {
        assertTrue(AppIdentity.isValidPackageName("com.android.chrome"));
        assertTrue(AppIdentity.isValidPackageName("com.google.android.calculator"));
        assertTrue(AppIdentity.isValidPackageName("com.example.app_v2"));
        assertTrue(AppIdentity.isValidPackageName("com.example._private"));
    }

    @Test
    public void isValidPackageName_trimsSurroundingWhitespace() {
        assertTrue(AppIdentity.isValidPackageName("  com.android.chrome  "));
    }

    @Test
    public void isValidPackageName_rejectsNullAndEmpty() {
        assertFalse(AppIdentity.isValidPackageName(null));
        assertFalse(AppIdentity.isValidPackageName(""));
        assertFalse(AppIdentity.isValidPackageName("   "));
    }

    @Test
    public void isValidPackageName_rejectsSingleSegment() {
        assertFalse(AppIdentity.isValidPackageName("chrome"));
    }

    @Test
    public void isValidPackageName_rejectsInvalidCharacters() {
        assertFalse(AppIdentity.isValidPackageName("com.android.chrome!"));
        assertFalse(AppIdentity.isValidPackageName("com.android chrome"));
        assertFalse(AppIdentity.isValidPackageName("com..android"));
        assertFalse(AppIdentity.isValidPackageName("com.android."));
        assertFalse(AppIdentity.isValidPackageName(".com.android"));
        assertFalse(AppIdentity.isValidPackageName("com.1android"));
    }

    @Test
    public void isValidPackageName_rejectsOverlyLongNames() {
        StringBuilder builder = new StringBuilder("com.");
        while (builder.length() < 300) {
            builder.append("segment.");
        }
        assertFalse(AppIdentity.isValidPackageName(builder.toString()));
    }
}
