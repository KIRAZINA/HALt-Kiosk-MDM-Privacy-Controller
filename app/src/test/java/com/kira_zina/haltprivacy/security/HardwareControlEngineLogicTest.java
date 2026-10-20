package com.kira_zina.haltprivacy.security;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.UserManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HardwareControlEngineLogicTest {

    /** The application id the engine is expected to lock down. */
    private static final String PACKAGE_NAME = "com.kira_zina.haltprivacy";

    @Mock private Context mockContext;
    @Mock private Context mockAppContext;
    @Mock private DevicePolicyManager mockDpm;

    private HardwareControlEngine engine;

    @Before
    public void setUp() {
        when(mockContext.getApplicationContext()).thenReturn(mockAppContext);
        when(mockContext.getSystemService(Context.DEVICE_POLICY_SERVICE)).thenReturn(mockDpm);
        when(mockDpm.isAdminActive(any(ComponentName.class))).thenReturn(true);
        engine = new HardwareControlEngine(mockContext);
    }

    @Test
    public void enableHardwarePrivacy_asDeviceOwner_callsCameraDisabledOnce() {
        when(mockDpm.isDeviceOwnerApp(anyString())).thenReturn(true);
        when(mockDpm.isProfileOwnerApp(anyString())).thenReturn(false);

        engine.enableHardwarePrivacy();

        verify(mockDpm, times(1)).setCameraDisabled(any(ComponentName.class), eq(true));
        verify(mockDpm).addUserRestriction(any(ComponentName.class), eq(UserManager.DISALLOW_UNMUTE_MICROPHONE));
        verify(mockDpm).addUserRestriction(any(ComponentName.class), eq(UserManager.DISALLOW_CAMERA_TOGGLE));
        verify(mockDpm).addUserRestriction(any(ComponentName.class), eq(UserManager.DISALLOW_MICROPHONE_TOGGLE));
    }

    @Test
    public void enableHardwarePrivacy_asProfileOwner_callsCameraDisabledOnce() {
        when(mockDpm.isDeviceOwnerApp(anyString())).thenReturn(false);
        when(mockDpm.isProfileOwnerApp(anyString())).thenReturn(true);

        engine.enableHardwarePrivacy();

        verify(mockDpm, times(1)).setCameraDisabled(any(ComponentName.class), eq(true));
        verify(mockDpm, never()).addUserRestriction(any(ComponentName.class), eq(UserManager.DISALLOW_UNMUTE_MICROPHONE));
        verify(mockDpm, never()).addUserRestriction(any(ComponentName.class), eq(UserManager.DISALLOW_MICROPHONE_TOGGLE));
    }

    @Test
    public void enableHardwarePrivacy_whenAdminNotActive_doesNothing() {
        when(mockDpm.isAdminActive(any(ComponentName.class))).thenReturn(false);

        engine.enableHardwarePrivacy();

        verify(mockDpm, never()).setCameraDisabled(any(ComponentName.class), anyBoolean());
    }

    @Test
    public void enableHardwarePrivacy_securityException_handledGracefully() {
        when(mockDpm.isDeviceOwnerApp(anyString())).thenReturn(true);
        when(mockDpm.isProfileOwnerApp(anyString())).thenReturn(false);
        doThrow(new SecurityException("Simulated security exception"))
                .when(mockDpm).setCameraDisabled(any(ComponentName.class), anyBoolean());

        engine.enableHardwarePrivacy();
    }

    @Test
    public void disableHardwarePrivacy_clearsAllRestrictions() {
        when(mockDpm.isDeviceOwnerApp(anyString())).thenReturn(true);

        engine.disableHardwarePrivacy();

        verify(mockDpm).setCameraDisabled(any(ComponentName.class), eq(false));
        verify(mockDpm).clearUserRestriction(any(ComponentName.class), eq(UserManager.DISALLOW_UNMUTE_MICROPHONE));
        verify(mockDpm).clearUserRestriction(any(ComponentName.class), eq(UserManager.DISALLOW_CAMERA_TOGGLE));
        verify(mockDpm).clearUserRestriction(any(ComponentName.class), eq(UserManager.DISALLOW_MICROPHONE_TOGGLE));
    }

    @Test
    public void isPrivacyModeEnforced_whenCameraDisabled_returnsTrue() {
        when(mockDpm.isAdminActive(any(ComponentName.class))).thenReturn(true);
        when(mockDpm.getCameraDisabled(any(ComponentName.class))).thenReturn(true);

        assertTrue(engine.isPrivacyModeEnforced());
    }

    @Test
    public void isPrivacyModeEnforced_whenCameraEnabled_returnsFalse() {
        when(mockDpm.isAdminActive(any(ComponentName.class))).thenReturn(true);
        when(mockDpm.getCameraDisabled(any(ComponentName.class))).thenReturn(false);

        assertFalse(engine.isPrivacyModeEnforced());
    }

    @Test
    public void isPrivacyModeEnforced_adminNotActive_returnsFalse() {
        when(mockDpm.isAdminActive(any(ComponentName.class))).thenReturn(false);

        assertFalse(engine.isPrivacyModeEnforced());
    }

    @Test
    public void isPrivacyModeEnforced_securityException_returnsFalse() {
        when(mockDpm.isAdminActive(any(ComponentName.class))).thenReturn(true);
        when(mockDpm.getCameraDisabled(any(ComponentName.class)))
                .thenThrow(new SecurityException("Simulated"));

        assertFalse(engine.isPrivacyModeEnforced());
    }

    @Test
    public void enableTamperProofing_whenDeviceOwner_appliesMeasures() {
        when(mockDpm.isDeviceOwnerApp(anyString())).thenReturn(true);
        when(mockDpm.isAdminActive(any(ComponentName.class))).thenReturn(true);

        assertTrue(engine.enableTamperProofing());

        verify(mockDpm).setUninstallBlocked(any(ComponentName.class), anyString(), eq(true));
        verify(mockDpm).setLockTaskPackages(any(ComponentName.class), any(String[].class));
    }

    @Test
    public void enableTamperProofing_whenNotDeviceOwner_doesNothing() {
        when(mockDpm.isDeviceOwnerApp(anyString())).thenReturn(false);

        assertFalse(engine.enableTamperProofing());

        verify(mockDpm, never()).setUninstallBlocked(any(ComponentName.class), anyString(), anyBoolean());
        verify(mockDpm, never()).setLockTaskPackages(any(ComponentName.class), any(String[].class));
        verify(mockDpm, never()).setLockTaskFeatures(any(ComponentName.class), anyInt());
    }

    @Test
    public void enableKioskMode_whenDeviceOwner_configuresPackagesAndFeatures() {
        when(mockDpm.isDeviceOwnerApp(anyString())).thenReturn(true);
        when(mockDpm.isAdminActive(any(ComponentName.class))).thenReturn(true);

        assertTrue(engine.enableKioskMode());

        verify(mockDpm).setLockTaskPackages(any(ComponentName.class), aryEq(new String[]{PACKAGE_NAME}));
        verify(mockDpm).setLockTaskFeatures(any(ComponentName.class),
                eq(DevicePolicyManager.LOCK_TASK_FEATURE_NONE));
    }

    @Test
    public void enableKioskMode_whenNotDeviceOwner_doesNothing() {
        when(mockDpm.isDeviceOwnerApp(anyString())).thenReturn(false);

        assertFalse(engine.enableKioskMode());

        verify(mockDpm, never()).setLockTaskPackages(any(ComponentName.class), any(String[].class));
        verify(mockDpm, never()).setLockTaskFeatures(any(ComponentName.class), anyInt());
    }

    @Test
    public void enableKioskMode_whenAdminNotActive_doesNothing() {
        when(mockDpm.isAdminActive(any(ComponentName.class))).thenReturn(false);

        assertFalse(engine.enableKioskMode());

        verify(mockDpm, never()).setLockTaskPackages(any(ComponentName.class), any(String[].class));
        verify(mockDpm, never()).setLockTaskFeatures(any(ComponentName.class), anyInt());
    }

    @Test
    public void enableKioskMode_whenSecurityException_returnsFalse() {
        when(mockDpm.isDeviceOwnerApp(anyString())).thenReturn(true);
        when(mockDpm.isAdminActive(any(ComponentName.class))).thenReturn(true);
        doThrow(new SecurityException("Simulated security exception"))
                .when(mockDpm).setLockTaskPackages(any(ComponentName.class), any(String[].class));

        assertFalse(engine.enableKioskMode());
    }

    @Test
    public void disableKioskMode_whenDeviceOwner_clearsPackagesAndResetsFeatures() {
        when(mockDpm.isDeviceOwnerApp(anyString())).thenReturn(true);
        when(mockDpm.isAdminActive(any(ComponentName.class))).thenReturn(true);

        assertTrue(engine.disableKioskMode());

        verify(mockDpm).setLockTaskPackages(any(ComponentName.class), aryEq(new String[]{}));
        verify(mockDpm).setLockTaskFeatures(any(ComponentName.class),
                eq(DevicePolicyManager.LOCK_TASK_FEATURE_NONE));
    }

    @Test
    public void disableKioskMode_whenNotDeviceOwner_doesNothing() {
        when(mockDpm.isDeviceOwnerApp(anyString())).thenReturn(false);

        assertFalse(engine.disableKioskMode());

        verify(mockDpm, never()).setLockTaskPackages(any(ComponentName.class), any(String[].class));
        verify(mockDpm, never()).setLockTaskFeatures(any(ComponentName.class), anyInt());
    }

    @Test
    public void disableTamperProofing_whenDeviceOwner_clearsLockTaskConfiguration() {
        when(mockDpm.isDeviceOwnerApp(anyString())).thenReturn(true);
        when(mockDpm.isAdminActive(any(ComponentName.class))).thenReturn(true);

        assertTrue(engine.disableTamperProofing());

        verify(mockDpm).setUninstallBlocked(any(ComponentName.class), anyString(), eq(false));
        verify(mockDpm).setLockTaskPackages(any(ComponentName.class), aryEq(new String[]{}));
        verify(mockDpm).setLockTaskFeatures(any(ComponentName.class),
                eq(DevicePolicyManager.LOCK_TASK_FEATURE_NONE));
    }

    @Test
    public void disableTamperProofing_whenNotDeviceOwner_doesNothing() {
        when(mockDpm.isDeviceOwnerApp(anyString())).thenReturn(false);

        assertFalse(engine.disableTamperProofing());

        verify(mockDpm, never()).setUninstallBlocked(any(ComponentName.class), anyString(), anyBoolean());
        verify(mockDpm, never()).setLockTaskPackages(any(ComponentName.class), any(String[].class));
    }

    @Test
    public void isKioskModeConfigured_whenPackageIsRegistered_returnsTrue() {
        when(mockDpm.getLockTaskPackages(any(ComponentName.class)))
                .thenReturn(new String[]{"com.example.other", PACKAGE_NAME});

        assertTrue(engine.isKioskModeConfigured());
    }

    @Test
    public void isKioskModeConfigured_whenPackageIsNotRegistered_returnsFalse() {
        when(mockDpm.getLockTaskPackages(any(ComponentName.class)))
                .thenReturn(new String[]{"com.example.other"});

        assertFalse(engine.isKioskModeConfigured());
    }

    @Test
    public void isKioskModeConfigured_whenNoPackagesRegistered_returnsFalse() {
        when(mockDpm.getLockTaskPackages(any(ComponentName.class))).thenReturn(new String[]{});

        assertFalse(engine.isKioskModeConfigured());
    }

    @Test
    public void isKioskModeConfigured_whenAdminNotActive_returnsFalse() {
        when(mockDpm.isAdminActive(any(ComponentName.class))).thenReturn(false);

        assertFalse(engine.isKioskModeConfigured());

        verify(mockDpm, never()).getLockTaskPackages(any(ComponentName.class));
    }

    @Test
    public void enableKioskMode_withAdditionalPackages_includesOwnAndExtras() {
        when(mockDpm.isDeviceOwnerApp(anyString())).thenReturn(true);
        when(mockDpm.isAdminActive(any(ComponentName.class))).thenReturn(true);

        Set<String> extras = new LinkedHashSet<>();
        extras.add("com.android.chrome");
        extras.add("com.google.android.calculator");

        assertTrue(engine.enableKioskMode(extras));

        verify(mockDpm).setLockTaskPackages(any(ComponentName.class),
                aryEq(new String[]{PACKAGE_NAME, "com.android.chrome", "com.google.android.calculator"}));
        verify(mockDpm).setLockTaskFeatures(any(ComponentName.class),
                eq(DevicePolicyManager.LOCK_TASK_FEATURE_NONE));
    }

    @Test
    public void enableKioskMode_withNullSet_includesOnlyOwnPackage() {
        when(mockDpm.isDeviceOwnerApp(anyString())).thenReturn(true);
        when(mockDpm.isAdminActive(any(ComponentName.class))).thenReturn(true);

        assertTrue(engine.enableKioskMode(null));

        verify(mockDpm).setLockTaskPackages(any(ComponentName.class), aryEq(new String[]{PACKAGE_NAME}));
    }

    @Test
    public void enableKioskMode_ignoresBlankEntriesAndOwnPackageDuplicates() {
        when(mockDpm.isDeviceOwnerApp(anyString())).thenReturn(true);
        when(mockDpm.isAdminActive(any(ComponentName.class))).thenReturn(true);

        Set<String> extras = new LinkedHashSet<>();
        extras.add("");
        extras.add("   ");
        extras.add(PACKAGE_NAME);
        extras.add("com.android.chrome");

        assertTrue(engine.enableKioskMode(extras));

        verify(mockDpm).setLockTaskPackages(any(ComponentName.class),
                aryEq(new String[]{PACKAGE_NAME, "com.android.chrome"}));
    }

    @Test
    public void enableTamperProofing_withAdditionalPackages_appliesFullKioskPackageSet() {
        when(mockDpm.isDeviceOwnerApp(anyString())).thenReturn(true);
        when(mockDpm.isAdminActive(any(ComponentName.class))).thenReturn(true);

        Set<String> extras = new LinkedHashSet<>();
        extras.add("com.android.chrome");

        assertTrue(engine.enableTamperProofing(extras));

        verify(mockDpm).setUninstallBlocked(any(ComponentName.class), anyString(), eq(true));
        verify(mockDpm).setLockTaskPackages(any(ComponentName.class),
                aryEq(new String[]{PACKAGE_NAME, "com.android.chrome"}));
    }

    @Test
    public void getAllowedKioskPackages_returnsPackagesFromDevicePolicyManager() {
        when(mockDpm.getLockTaskPackages(any(ComponentName.class)))
                .thenReturn(new String[]{PACKAGE_NAME, "com.android.chrome"});

        Set<String> packages = engine.getAllowedKioskPackages();

        assertEquals(2, packages.size());
        assertTrue(packages.contains(PACKAGE_NAME));
        assertTrue(packages.contains("com.android.chrome"));
    }

    @Test
    public void getAllowedKioskPackages_whenNoPackagesRegistered_returnsEmptySet() {
        when(mockDpm.getLockTaskPackages(any(ComponentName.class))).thenReturn(new String[]{});

        assertTrue(engine.getAllowedKioskPackages().isEmpty());
    }

    @Test
    public void getAllowedKioskPackages_whenAdminNotActive_returnsEmptySet() {
        when(mockDpm.isAdminActive(any(ComponentName.class))).thenReturn(false);

        assertTrue(engine.getAllowedKioskPackages().isEmpty());

        verify(mockDpm, never()).getLockTaskPackages(any(ComponentName.class));
    }

    @Test
    public void getAllowedKioskPackages_whenSecurityException_returnsEmptySet() {
        when(mockDpm.getLockTaskPackages(any(ComponentName.class)))
                .thenThrow(new SecurityException("Simulated"));

        assertTrue(engine.getAllowedKioskPackages().isEmpty());
    }

    @Test
    public void disableKioskMode_resetsAllowedPackagesToEmpty() {
        when(mockDpm.isDeviceOwnerApp(anyString())).thenReturn(true);
        when(mockDpm.isAdminActive(any(ComponentName.class))).thenReturn(true);

        assertTrue(engine.disableKioskMode());

        verify(mockDpm).setLockTaskPackages(any(ComponentName.class), aryEq(new String[]{}));
    }
}
