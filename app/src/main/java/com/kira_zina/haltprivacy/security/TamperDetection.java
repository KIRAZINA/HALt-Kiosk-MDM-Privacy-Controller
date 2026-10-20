package com.kira_zina.haltprivacy.security;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.util.Log;

import com.kira_zina.haltprivacy.BuildConfig;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Verifies that the running APK was signed by the expected certificate.
 *
 * <p>A Device Policy Controller that disables the camera is a high-value target:
 * an attacker can repackage it with a permissive {@code device_admin_policies.xml}
 * and re-enrol it. Comparing the signing certificate's SHA-256 against a known
 * value detects that class of tampering.</p>
 *
 * <p>Verification is enforced only when the expected hash has been configured.
 * Debug builds, and release builds that still carry the placeholder constant,
 * bypass the check with a prominent warning so that development is not blocked
 * by a value that can only be produced after the release key exists.</p>
 */
public class TamperDetection {
    private static final String TAG = "TamperDetection";

    /**
     * SHA-256 of the release signing certificate, uppercase hex, 64 characters.
     * Replace the placeholder with the real value to enforce the check.
     */
    static final String EXPECTED_SIGNATURE_HASH = "YOUR_64_CHAR_HEX_HASH_HERE";

    /** Marker that identifies an unconfigured expected hash. */
    private static final String UNSET_HASH_MARKER = "YOUR_64_CHAR_HEX_HASH_HERE";

    /**
     * Verifies the APK signature against {@link #EXPECTED_SIGNATURE_HASH}.
     *
     * @param context context used to resolve the installed package
     * @return true when the signature is trusted, or when the check is bypassed
     */
    public static boolean verifyAppSignature(Context context) {
        return verifyAppSignature(context, BuildConfig.DEBUG);
    }

    /**
     * @param debugBuild when true the strict check is bypassed
     * @return true when the signature is trusted, or when the check is bypassed
     */
    static boolean verifyAppSignature(Context context, boolean debugBuild) {
        if (debugBuild) {
            Log.w(TAG, "Running in DEBUG mode — bypass signature check or use calculated hash");
            return true;
        }

        if (isExpectedHashUnset()) {
            Log.w(TAG, "EXPECTED_SIGNATURE_HASH is not configured — signature verification bypassed. "
                    + "Set the release signing certificate SHA-256 to enforce tamper detection.");
            return true;
        }

        return verifyAppSignatureStrict(context, EXPECTED_SIGNATURE_HASH);
    }

    /**
     * Performs the raw certificate comparison with no bypass.
     *
     * @param expectedHash the expected certificate hash, uppercase hex
     * @return true only when a signer of the APK matches {@code expectedHash}
     */
    static boolean verifyAppSignatureStrict(Context context, String expectedHash) {
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

                if (expectedHash != null && expectedHash.equalsIgnoreCase(hash)) {
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

    /**
     * @return true when the expected hash constant is still the unconfigured placeholder
     */
    static boolean isExpectedHashUnset() {
        return EXPECTED_SIGNATURE_HASH == null
                || EXPECTED_SIGNATURE_HASH.isEmpty()
                || EXPECTED_SIGNATURE_HASH.equals(UNSET_HASH_MARKER);
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
