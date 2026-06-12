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

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HardwareControlEngineLogicTest {

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

        engine.enableTamperProofing();

        verify(mockDpm).setUninstallBlocked(any(ComponentName.class), anyString(), eq(true));
        verify(mockDpm).setLockTaskPackages(any(ComponentName.class), any(String[].class));
    }

    @Test
    public void enableTamperProofing_whenNotDeviceOwner_doesNothing() {
        when(mockDpm.isDeviceOwnerApp(anyString())).thenReturn(false);

        engine.enableTamperProofing();

        verify(mockDpm, never()).setUninstallBlocked(any(ComponentName.class), anyString(), anyBoolean());
    }
}
