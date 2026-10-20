package com.kira_zina.haltprivacy.security;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PrefsManagerTest {

    private static final String PREF_NAME = "halt_privacy_prefs";
    private static final String KEY_LOCKOUT_END = "lockout_end_timestamp_ms";
    private static final String KEY_KIOSK_CONFIGURED = "kiosk_mode_configured";
    private static final String KEY_ALLOWED_PACKAGES = "allowed_kiosk_packages";

    @Mock private Context mockContext;
    @Mock private Context mockAppContext;
    @Mock private SharedPreferences mockPrefs;
    @Mock private SharedPreferences.Editor mockEditor;

    private PrefsManager prefsManager;

    @Before
    public void setUp() {
        when(mockContext.getApplicationContext()).thenReturn(mockAppContext);
        when(mockAppContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)).thenReturn(mockPrefs);
        when(mockPrefs.edit()).thenReturn(mockEditor);
        when(mockEditor.putLong(anyString(), anyLong())).thenReturn(mockEditor);
        when(mockEditor.putBoolean(anyString(), anyBoolean())).thenReturn(mockEditor);
        when(mockEditor.putStringSet(eq(KEY_ALLOWED_PACKAGES), anySet())).thenReturn(mockEditor);
        when(mockEditor.remove(anyString())).thenReturn(mockEditor);

        prefsManager = new PrefsManager(mockContext);
    }

    @Test
    public void constructor_usesPrivateModeAndTheApplicationContext() {
        verify(mockAppContext).getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    @Test
    public void getLockoutEndTimestamp_returnsZeroWhenNotStored() {
        when(mockPrefs.getLong(KEY_LOCKOUT_END, 0L)).thenReturn(0L);

        assertEquals(0L, prefsManager.getLockoutEndTimestamp());
    }

    @Test
    public void setLockoutEndTimestamp_storesTheValue() {
        prefsManager.setLockoutEndTimestamp(1_700_000_000_000L);

        verify(mockEditor).putLong(KEY_LOCKOUT_END, 1_700_000_000_000L);
        verify(mockEditor).apply();
    }

    @Test
    public void clearLockoutEndTimestamp_removesTheValue() {
        prefsManager.clearLockoutEndTimestamp();

        verify(mockEditor).remove(KEY_LOCKOUT_END);
        verify(mockEditor).apply();
    }

    @Test
    public void isTimedLockoutActive_isTrueWhileTheDeadlineIsInTheFuture() {
        when(mockPrefs.getLong(KEY_LOCKOUT_END, 0L))
                .thenReturn(System.currentTimeMillis() + 60_000L);

        assertTrue(prefsManager.isTimedLockoutActive());
    }

    @Test
    public void isTimedLockoutActive_isFalseWhenTheDeadlineHasPassed() {
        when(mockPrefs.getLong(KEY_LOCKOUT_END, 0L))
                .thenReturn(System.currentTimeMillis() - 1_000L);

        assertFalse(prefsManager.isTimedLockoutActive());
    }

    @Test
    public void isTimedLockoutActive_isFalseWhenNoLockoutIsStored() {
        when(mockPrefs.getLong(KEY_LOCKOUT_END, 0L)).thenReturn(0L);

        assertFalse(prefsManager.isTimedLockoutActive());
    }

    @Test
    public void setKioskModeConfigured_storesTheFlag() {
        prefsManager.setKioskModeConfigured(true);

        verify(mockEditor).putBoolean(KEY_KIOSK_CONFIGURED, true);
    }

    @Test
    public void allowedKioskPackages_areEmptyWhenNothingIsStored() {
        when(mockPrefs.getStringSet(KEY_ALLOWED_PACKAGES, null)).thenReturn(null);

        assertNotNull(prefsManager.getAllowedKioskPackages());
        assertTrue(prefsManager.getAllowedKioskPackages().isEmpty());
    }

    @Test
    public void allowedKioskPackages_areCopiedInAndOut() {
        when(mockPrefs.getStringSet(KEY_ALLOWED_PACKAGES, null))
                .thenReturn(Collections.singleton("com.android.chrome"));

        assertTrue(prefsManager.getAllowedKioskPackages().contains("com.android.chrome"));

        prefsManager.setAllowedKioskPackages(Collections.singleton("com.android.chrome"));

        verify(mockEditor).putStringSet(eq(KEY_ALLOWED_PACKAGES), anySet());
    }
}
