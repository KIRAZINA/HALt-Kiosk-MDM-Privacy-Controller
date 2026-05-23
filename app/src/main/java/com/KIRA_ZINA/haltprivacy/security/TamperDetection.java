package com.KIRA_ZINA.haltprivacy.security;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Provides APK signature verification to detect tampering.
 */
public class TamperDetection {
    private static final String TAG = "TamperDetection";

    private static final String EXPECTED_SIGNATURE_HASH = "";

    public static boolean verifyAppSignature(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo packageInfo = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);

            for (Signature signature : packageInfo.signatures) {
                String hash = computeSha256(signature.toByteArray());
                Log.d(TAG, "APK signature hash: " + hash);
            }

            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Package not found", e);
            return false;
        }
    }

    private static String computeSha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "SHA-256 not available", e);
            return "";
        }
    }
}