package com.kira_zina.haltprivacy.security;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.security.MessageDigest;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class TamperDetectionTest {

    @Mock private Context mockContext;
    @Mock private PackageManager mockPm;
    @Mock private SigningInfo mockSigningInfo;
    @Mock private Signature mockSignature;

    private PackageInfo packageInfo;

    @Before
    public void setUp() throws Exception {
        when(mockContext.getPackageName()).thenReturn("com.kira_zina.haltprivacy");
        when(mockContext.getPackageManager()).thenReturn(mockPm);

        packageInfo = new PackageInfo();
        packageInfo.signingInfo = mockSigningInfo;

        when(mockPm.getPackageInfo(anyString(), anyInt())).thenReturn(packageInfo);
        when(mockSigningInfo.hasMultipleSigners()).thenReturn(false);
        when(mockSigningInfo.getSigningCertificateHistory()).thenReturn(new Signature[]{mockSignature});
    }

    @Test
    public void verifyAppSignature_whenHashMismatch_returnsFalse() {
        when(mockSignature.toByteArray()).thenReturn("anything".getBytes());
        assertFalse(TamperDetection.verifyAppSignature(mockContext));
    }

    @Test
    public void verifyAppSignature_whenNoSigningInfo_returnsFalse() {
        packageInfo.signingInfo = null;
        assertFalse(TamperDetection.verifyAppSignature(mockContext));
    }

    @Test
    public void computeSha256_returns64HexChars() {
        String hash = invokeComputeSha256("test".getBytes());
        assertNotNull(hash);
        assertEquals(64, hash.length());
    }

    @Test
    public void computeSha256_isDeterministic() {
        byte[] data = "test".getBytes();
        assertEquals(invokeComputeSha256(data), invokeComputeSha256(data));
    }

    @Test
    public void computeSha256_knownValue() throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest("hello".getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02X", b));
        }
        String expected = sb.toString();
        assertEquals(expected, invokeComputeSha256("hello".getBytes()));
    }

    private static String invokeComputeSha256(byte[] data) {
        try {
            java.lang.reflect.Method method = TamperDetection.class.getDeclaredMethod("computeSha256", byte[].class);
            method.setAccessible(true);
            return (String) method.invoke(null, data);
        } catch (NoSuchMethodException e) {
            throw new AssertionError("computeSha256 method not found. Ensure it exists with signature: private static String computeSha256(byte[])", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke computeSha256", e);
        }
    }
}
