package com.kira_zina.haltprivacy.security;

import android.content.Context;
import android.content.RestrictionsManager;
import android.os.Bundle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ManagedConfigManagerTest {

    @Mock private Context mockContext;
    @Mock private Context mockAppContext;
    @Mock private RestrictionsManager mockRestrictionsManager;

    @Before
    public void setUp() {
        when(mockContext.getApplicationContext()).thenReturn(mockAppContext);
        when(mockAppContext.getSystemService(Context.RESTRICTIONS_SERVICE))
                .thenReturn(mockRestrictionsManager);
    }

    // ---------------------------------------------------------------- parsing

    @Test
    public void parse_withNullBundle_returnsTheDisabledDefaults() {
        ManagedConfig config = ManagedConfigManager.parse(null);

        assertNotNull(config);
        assertFalse(config.isHardwarePrivacyEnabled());
        assertFalse(config.isKioskModeEnabled());
        assertEquals(0, config.getTimedLockoutMinutes());
        assertTrue(config.getAllowedPackages().isEmpty());
        assertTrue(config.isEmpty());
    }

    @Test
    public void parse_withEmptyBundle_returnsTheDisabledDefaults() {
        Bundle bundle = mock(Bundle.class);
        when(bundle.getBoolean(ManagedConfigManager.KEY_HARDWARE_PRIVACY_ENABLED, false))
                .thenReturn(false);
        when(bundle.getBoolean(ManagedConfigManager.KEY_KIOSK_MODE_ENABLED, false))
                .thenReturn(false);
        when(bundle.getStringArray(ManagedConfigManager.KEY_KIOSK_ALLOWED_PACKAGES))
                .thenReturn(new String[]{});
        when(bundle.getInt(ManagedConfigManager.KEY_TIMED_LOCKOUT_DURATION_MINUTES, 0))
                .thenReturn(0);

        ManagedConfig config = ManagedConfigManager.parse(bundle);

        assertFalse(config.isHardwarePrivacyEnabled());
        assertFalse(config.isKioskModeEnabled());
        assertEquals(0, config.getTimedLockoutMinutes());
        assertTrue(config.getAllowedPackages().isEmpty());
        assertTrue(config.isEmpty());
    }

    @Test
    public void parse_withPopulatedBundle_readsEveryValue() {
        Bundle bundle = mock(Bundle.class);
        when(bundle.getBoolean(ManagedConfigManager.KEY_HARDWARE_PRIVACY_ENABLED, false))
                .thenReturn(true);
        when(bundle.getBoolean(ManagedConfigManager.KEY_KIOSK_MODE_ENABLED, false))
                .thenReturn(true);
        when(bundle.getStringArray(ManagedConfigManager.KEY_KIOSK_ALLOWED_PACKAGES))
                .thenReturn(new String[]{"com.android.chrome", "com.google.android.calculator"});
        when(bundle.getInt(ManagedConfigManager.KEY_TIMED_LOCKOUT_DURATION_MINUTES, 0))
                .thenReturn(45);

        ManagedConfig config = ManagedConfigManager.parse(bundle);

        assertTrue(config.isHardwarePrivacyEnabled());
        assertTrue(config.isKioskModeEnabled());
        assertEquals(45, config.getTimedLockoutMinutes());
        assertEquals(2, config.getAllowedPackages().size());
        assertTrue(config.getAllowedPackages().contains("com.android.chrome"));
        assertTrue(config.getAllowedPackages().contains("com.google.android.calculator"));
        assertFalse(config.isEmpty());
    }

    @Test
    public void parse_withMalformedValues_fallsBackToTheDefaults() {
        Bundle bundle = mock(Bundle.class);
        when(bundle.getBoolean(ManagedConfigManager.KEY_HARDWARE_PRIVACY_ENABLED, false))
                .thenThrow(new ClassCastException("expected boolean"));
        when(bundle.getBoolean(ManagedConfigManager.KEY_KIOSK_MODE_ENABLED, false))
                .thenThrow(new ClassCastException("expected boolean"));
        when(bundle.getStringArray(ManagedConfigManager.KEY_KIOSK_ALLOWED_PACKAGES))
                .thenThrow(new ClassCastException("expected string array"));
        when(bundle.getInt(ManagedConfigManager.KEY_TIMED_LOCKOUT_DURATION_MINUTES, 0))
                .thenThrow(new ClassCastException("expected integer"));

        ManagedConfig config = ManagedConfigManager.parse(bundle);

        assertFalse(config.isHardwarePrivacyEnabled());
        assertFalse(config.isKioskModeEnabled());
        assertEquals(0, config.getTimedLockoutMinutes());
        assertTrue(config.getAllowedPackages().isEmpty());
    }

    @Test
    public void parse_withNullStringArray_returnsAnEmptyAllowlist() {
        Bundle bundle = mock(Bundle.class);
        when(bundle.getBoolean(ManagedConfigManager.KEY_HARDWARE_PRIVACY_ENABLED, false))
                .thenReturn(false);
        when(bundle.getBoolean(ManagedConfigManager.KEY_KIOSK_MODE_ENABLED, false))
                .thenReturn(false);
        when(bundle.getStringArray(ManagedConfigManager.KEY_KIOSK_ALLOWED_PACKAGES))
                .thenReturn(null);
        when(bundle.getInt(ManagedConfigManager.KEY_TIMED_LOCKOUT_DURATION_MINUTES, 0))
                .thenReturn(0);

        ManagedConfig config = ManagedConfigManager.parse(bundle);

        assertNotNull(config.getAllowedPackages());
        assertTrue(config.getAllowedPackages().isEmpty());
    }

    @Test
    public void parse_withBlankPackageEntries_dropsThem() {
        Bundle bundle = mock(Bundle.class);
        when(bundle.getBoolean(ManagedConfigManager.KEY_HARDWARE_PRIVACY_ENABLED, false))
                .thenReturn(false);
        when(bundle.getBoolean(ManagedConfigManager.KEY_KIOSK_MODE_ENABLED, false))
                .thenReturn(false);
        when(bundle.getStringArray(ManagedConfigManager.KEY_KIOSK_ALLOWED_PACKAGES))
                .thenReturn(new String[]{"  com.android.chrome  ", "", "   "});
        when(bundle.getInt(ManagedConfigManager.KEY_TIMED_LOCKOUT_DURATION_MINUTES, 0))
                .thenReturn(0);

        ManagedConfig config = ManagedConfigManager.parse(bundle);

        assertEquals(1, config.getAllowedPackages().size());
        assertTrue(config.getAllowedPackages().contains("com.android.chrome"));
    }

    @Test
    public void parse_withNegativeDuration_clampsToZero() {
        Bundle bundle = mock(Bundle.class);
        when(bundle.getBoolean(ManagedConfigManager.KEY_HARDWARE_PRIVACY_ENABLED, false))
                .thenReturn(false);
        when(bundle.getBoolean(ManagedConfigManager.KEY_KIOSK_MODE_ENABLED, false))
                .thenReturn(false);
        when(bundle.getStringArray(ManagedConfigManager.KEY_KIOSK_ALLOWED_PACKAGES))
                .thenReturn(new String[]{});
        when(bundle.getInt(ManagedConfigManager.KEY_TIMED_LOCKOUT_DURATION_MINUTES, 0))
                .thenReturn(-15);

        ManagedConfig config = ManagedConfigManager.parse(bundle);

        assertEquals(0, config.getTimedLockoutMinutes());
    }

    // ---------------------------------------------------------------- reading

    @Test
    public void readManagedConfig_readsTheBundleFromTheRestrictionsManager() {
        Bundle bundle = mock(Bundle.class);
        when(bundle.getBoolean(ManagedConfigManager.KEY_HARDWARE_PRIVACY_ENABLED, false))
                .thenReturn(true);
        when(bundle.getBoolean(ManagedConfigManager.KEY_KIOSK_MODE_ENABLED, false))
                .thenReturn(false);
        when(bundle.getStringArray(ManagedConfigManager.KEY_KIOSK_ALLOWED_PACKAGES))
                .thenReturn(new String[]{"com.android.chrome"});
        when(bundle.getInt(ManagedConfigManager.KEY_TIMED_LOCKOUT_DURATION_MINUTES, 0))
                .thenReturn(10);
        when(mockRestrictionsManager.getApplicationRestrictions()).thenReturn(bundle);

        ManagedConfig config = new ManagedConfigManager(mockContext).readManagedConfig();

        assertTrue(config.isHardwarePrivacyEnabled());
        assertEquals(10, config.getTimedLockoutMinutes());
        assertEquals(1, config.getAllowedPackages().size());
    }

    @Test
    public void readManagedConfig_withoutARestrictionsManager_returnsTheDefaults() {
        when(mockAppContext.getSystemService(Context.RESTRICTIONS_SERVICE)).thenReturn(null);

        ManagedConfig config = new ManagedConfigManager(mockContext).readManagedConfig();

        assertNotNull(config);
        assertTrue(config.isEmpty());
    }

    @Test
    public void readManagedConfig_withoutRestrictions_returnsTheDefaults() {
        when(mockRestrictionsManager.getApplicationRestrictions()).thenReturn(null);

        ManagedConfig config = new ManagedConfigManager(mockContext).readManagedConfig();

        assertNotNull(config);
        assertTrue(config.isEmpty());
    }

    // ---------------------------------------------------------------- model

    @Test
    public void managedConfig_setAllowedPackages_dropsBlankEntries() {
        ManagedConfig config = new ManagedConfig();
        config.setAllowedPackages(Arrays.asList("com.android.chrome", "  ", null));

        assertEquals(1, config.getAllowedPackages().size());
    }

    @Test
    public void managedConfig_getAllowedPackages_isUnmodifiable() {
        ManagedConfig config = new ManagedConfig();

        try {
            config.getAllowedPackages().add("com.android.chrome");
            org.junit.Assert.fail("The allowlist must not be modifiable from the outside.");
        } catch (UnsupportedOperationException expected) {
            // intended
        }
    }

    @Test
    public void managedConfig_toString_mentionsEveryField() {
        ManagedConfig config = new ManagedConfig();
        config.setHardwarePrivacyEnabled(true);
        config.setKioskModeEnabled(true);
        config.setTimedLockoutMinutes(5);
        config.setAllowedPackages(java.util.Collections.singleton("com.android.chrome"));

        String description = config.toString();

        assertTrue(description.contains("hardwarePrivacy=true"));
        assertTrue(description.contains("kioskMode=true"));
        assertTrue(description.contains("timedLockoutMinutes=5"));
        assertTrue(description.contains("com.android.chrome"));
    }
}
