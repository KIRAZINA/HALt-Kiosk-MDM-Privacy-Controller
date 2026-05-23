package com.KIRA_ZINA.haltprivacy.provisioning;

import org.junit.Test;

import static org.junit.Assert.*;

public class ProvisioningEngineLogicTest {

    @Test
    public void testProvisioningResult_enumValues() {
        ProvisioningEngine.ProvisioningResult[] values = ProvisioningEngine.ProvisioningResult.values();
        assertEquals(4, values.length);
        assertEquals("DEVICE_OWNER", values[0].name());
        assertEquals("PROFILE_OWNER", values[1].name());
        assertEquals("PROVISIONING_INITIATED", values[2].name());
        assertEquals("FAILED", values[3].name());
    }

    @Test
    public void testProvisioningResult_valuesAreSingleton() {
        assertSame(ProvisioningEngine.ProvisioningResult.DEVICE_OWNER,
                ProvisioningEngine.ProvisioningResult.valueOf("DEVICE_OWNER"));
        assertSame(ProvisioningEngine.ProvisioningResult.PROFILE_OWNER,
                ProvisioningEngine.ProvisioningResult.valueOf("PROFILE_OWNER"));
        assertSame(ProvisioningEngine.ProvisioningResult.PROVISIONING_INITIATED,
                ProvisioningEngine.ProvisioningResult.valueOf("PROVISIONING_INITIATED"));
        assertSame(ProvisioningEngine.ProvisioningResult.FAILED,
                ProvisioningEngine.ProvisioningResult.valueOf("FAILED"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testProvisioningResult_invalidValue_throwsException() {
        ProvisioningEngine.ProvisioningResult.valueOf("INVALID");
    }

    @Test
    public void testProvisioningResult_ordinals() {
        assertEquals(0, ProvisioningEngine.ProvisioningResult.DEVICE_OWNER.ordinal());
        assertEquals(1, ProvisioningEngine.ProvisioningResult.PROFILE_OWNER.ordinal());
        assertEquals(2, ProvisioningEngine.ProvisioningResult.PROVISIONING_INITIATED.ordinal());
        assertEquals(3, ProvisioningEngine.ProvisioningResult.FAILED.ordinal());
    }

    @Test
    public void testProvisioningResult_equals() {
        assertEquals(ProvisioningEngine.ProvisioningResult.DEVICE_OWNER,
                ProvisioningEngine.ProvisioningResult.valueOf("DEVICE_OWNER"));
    }

    @Test
    public void testProvisioningResult_notEquals() {
        assertNotEquals(ProvisioningEngine.ProvisioningResult.DEVICE_OWNER,
                ProvisioningEngine.ProvisioningResult.PROFILE_OWNER);
    }
}