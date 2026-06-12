package com.kira_zina.haltprivacy.provisioning;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ProvisioningEngineLogicTest {

    @Mock private Context mockContext;
    @Mock private Context mockAppContext;
    @Mock private DevicePolicyManager mockDpm;
    @Mock private AccountManager mockAccountManager;
    @Mock private PackageManager mockPm;

    private ProvisioningEngine engine;

    @Before
    public void setUp() {
        when(mockContext.getApplicationContext()).thenReturn(mockAppContext);
        when(mockContext.getSystemService(Context.DEVICE_POLICY_SERVICE)).thenReturn(mockDpm);
        when(mockAppContext.getSystemService(Context.DEVICE_POLICY_SERVICE)).thenReturn(mockDpm);
        when(mockAppContext.getSystemService(Context.ACCOUNT_SERVICE)).thenReturn(mockAccountManager);
        when(mockAppContext.getPackageManager()).thenReturn(mockPm);
        engine = new ProvisioningEngine(mockContext);
    }

    @Test
    public void executeProvisioning_whenAlreadyDeviceOwner_returnsDeviceOwner() {
        when(mockDpm.isDeviceOwnerApp(anyString())).thenReturn(true);

        ProvisioningEngine.ProvisioningResult result = engine.executeProvisioning();

        assertEquals(ProvisioningEngine.ProvisioningResult.DEVICE_OWNER, result);
    }

    @Test
    public void executeProvisioning_whenAlreadyProfileOwner_returnsProfileOwner() {
        when(mockDpm.isDeviceOwnerApp(anyString())).thenReturn(false);
        when(mockDpm.isProfileOwnerApp(anyString())).thenReturn(true);

        ProvisioningEngine.ProvisioningResult result = engine.executeProvisioning();

        assertEquals(ProvisioningEngine.ProvisioningResult.PROFILE_OWNER, result);
    }

    @Test
    public void executeProvisioning_whenNotProvisioned_returnsFailed() {
        when(mockDpm.isDeviceOwnerApp(anyString())).thenReturn(false);
        when(mockDpm.isProfileOwnerApp(anyString())).thenReturn(false);

        ProvisioningEngine.ProvisioningResult result = engine.executeProvisioning();

        assertEquals(ProvisioningEngine.ProvisioningResult.FAILED, result);
    }

    @Test
    public void executeProvisioning_whenBlockingAccounts_returnsFailed() {
        when(mockDpm.isDeviceOwnerApp(anyString())).thenReturn(false);
        when(mockDpm.isProfileOwnerApp(anyString())).thenReturn(false);
        when(mockAccountManager.getAccountsByType("com.google")).thenReturn(new Account[]{
                new Account("test@gmail.com", "com.google")
        });

        ProvisioningEngine.ProvisioningResult result = engine.executeProvisioning();

        assertEquals(ProvisioningEngine.ProvisioningResult.FAILED, result);
    }

    @Test
    public void getEnterpriseProvisioningIntent_returnsNull_whenPmUnavailable() {
        when(mockAppContext.getPackageManager()).thenReturn(null);

        Intent intent = engine.getEnterpriseProvisioningIntent();

        assertNull(intent);
    }

    @Test
    public void provisioningResult_enumValues() {
        ProvisioningEngine.ProvisioningResult[] values = ProvisioningEngine.ProvisioningResult.values();
        assertEquals(4, values.length);
        assertEquals("DEVICE_OWNER", values[0].name());
        assertEquals("PROFILE_OWNER", values[1].name());
        assertEquals("PROVISIONING_INITIATED", values[2].name());
        assertEquals("FAILED", values[3].name());
    }
}
