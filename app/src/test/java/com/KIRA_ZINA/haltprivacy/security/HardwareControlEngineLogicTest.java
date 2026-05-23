package com.KIRA_ZINA.haltprivacy.security;

import org.junit.Test;

import static org.junit.Assert.*;

public class HardwareControlEngineLogicTest {

    @Test
    public void testProvisioningResult_allValuesExist() {
        assertNotNull(com.KIRA_ZINA.haltprivacy.provisioning.ProvisioningEngine.ProvisioningResult.DEVICE_OWNER);
        assertNotNull(com.KIRA_ZINA.haltprivacy.provisioning.ProvisioningEngine.ProvisioningResult.PROFILE_OWNER);
        assertNotNull(com.KIRA_ZINA.haltprivacy.provisioning.ProvisioningEngine.ProvisioningResult.PROVISIONING_INITIATED);
        assertNotNull(com.KIRA_ZINA.haltprivacy.provisioning.ProvisioningEngine.ProvisioningResult.FAILED);
        assertEquals(4, com.KIRA_ZINA.haltprivacy.provisioning.ProvisioningEngine.ProvisioningResult.values().length);
    }

    @Test
    public void testSecureServiceApplicationClass_exists() {
        assertNotNull(com.KIRA_ZINA.haltprivacy.SecureServiceApplication.class);
    }

    @Test
    public void testProvisioningEngineClass_exists() {
        assertNotNull(com.KIRA_ZINA.haltprivacy.provisioning.ProvisioningEngine.class);
    }

    @Test
    public void testHardwareControlEngineClass_exists() {
        assertNotNull(com.KIRA_ZINA.haltprivacy.security.HardwareControlEngine.class);
    }

    @Test
    public void testEnterpriseSecurityReceiverClass_exists() {
        assertNotNull(com.KIRA_ZINA.haltprivacy.security.EnterpriseSecurityReceiver.class);
    }

    @Test
    public void testBootCompletedReceiverClass_exists() {
        assertNotNull(com.KIRA_ZINA.haltprivacy.security.BootCompletedReceiver.class);
    }

    @Test
    public void testProvisioningActivityClass_exists() {
        assertNotNull(com.KIRA_ZINA.haltprivacy.provisioning.ProvisioningActivity.class);
    }

    @Test
    public void testProvisioningViewModelClass_exists() {
        assertNotNull(com.KIRA_ZINA.haltprivacy.ui.ProvisioningViewModel.class);
    }

    @Test
    public void testTamperDetectionClass_exists() {
        assertNotNull(com.KIRA_ZINA.haltprivacy.security.TamperDetection.class);
    }

    @Test
    public void testActivityClass_exists() {
        assertNotNull(com.KIRA_ZINA.haltprivacy.ui.MainActivity.class);
    }
}