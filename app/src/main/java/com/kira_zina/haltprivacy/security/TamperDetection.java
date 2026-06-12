package com.kira_zina.haltprivacy.security;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class TamperDetection {
    private static final String TAG = "TamperDetection";
    static final String EXPECTED_SIGNATURE_HASH = "YOUR_64_CHAR_HEX_HASH_HERE";

    public static boolean verifyAppSignature(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo packageInfo = pm.getPackageInfo(
                    context.getPackageName(),
                    PackageManager.GET_SIGNING_CERTIFICATES
            );

            SigningInfo signingInfo = packageInfo.signingInfo;
            if (signingInfo == null) {
                Log.e(TAG, "No signing info available.");
                return false;
            }

            Signature[] signatures = signingInfo.hasMultipleSigners()
                    ? signingInfo.getApkContentsSigners()
                    : signingInfo.getSigningCertificateHistory();

            if (signatures == null || signatures.length == 0) {
                Log.e(TAG, "No signers found in APK.");
                return false;
            }

            for (Signature signature : signatures) {
                String hash = computeSha256(signature.toByteArray());
                Log.d(TAG, "APK signature hash: " + hash);

                if (EXPECTED_SIGNATURE_HASH.equalsIgnoreCase(hash)) {
                    Log.i(TAG, "APK signature verified successfully.");
                    return true;
                }
            }

            Log.e(TAG, "TAMPER DETECTED: Signature mismatch!");
            return false;

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
                sb.append(String.format("%02X", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "SHA-256 not available", e);
            return "";
        }
    }
}
