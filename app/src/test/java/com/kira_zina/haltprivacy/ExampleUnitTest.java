package com.kira_zina.haltprivacy;

import com.kira_zina.haltprivacy.provisioning.ProvisioningEngine;

import org.junit.Test;

import static org.junit.Assert.*;

public class ExampleUnitTest {
    @Test
    public void provisioningResult_hasFourValues() {
        assertEquals(4, ProvisioningEngine.ProvisioningResult.values().length);
    }

    @Test
    public void provisioningResult_deviceOwnerIsFirst() {
        assertEquals("DEVICE_OWNER", ProvisioningEngine.ProvisioningResult.values()[0].name());
    }
}
