package com.kira_zina.haltprivacy.provisioning;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.kira_zina.haltprivacy.R;
import com.kira_zina.haltprivacy.security.EnterpriseSecurityReceiver;
import com.kira_zina.haltprivacy.security.HardwareControlEngine;
import com.kira_zina.haltprivacy.ui.MainActivity;

/**
 * The enrollment handshake of the Android Enterprise provisioning wizard.
 *
 * <p>The wizard launches this activity because the manifest declares the
 * {@code android.app.action.PROVISION_MANAGED_DEVICE} intent filter. The
 * activity confirms the enrollment, applies the privacy policy and closes the
 * handshake with {@link #RESULT_OK} so that the wizard can finish its own
 * flow.</p>
 *
 * <p>The device administrator is activated asynchronously, so the role is
 * polled for a short while before the handshake is given up on.</p>
 */
public class ProvisioningActivity extends AppCompatActivity {
    private static final String TAG = "ProvisioningActivity";

    /** How often the enrollment state is checked. */
    private static final long POLL_INTERVAL_MS = 300L;

    /** How long the handshake waits for the role to be granted. */
    private static final long PROVISIONING_TIMEOUT_MS = 15_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private HardwareControlEngine hardwareEngine;
    private TextView messageView;
    private long deadline;
    private boolean handshakeClosed;

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (handshakeClosed) {
                return;
            }
            if (isEnrolled()) {
                completeProvisioning();
                return;
            }
            if (SystemClock.elapsedRealtime() >= deadline) {
                Log.w(TAG, "Enrollment was not confirmed within the timeout.");
                showMessage(getString(R.string.provisioning_not_confirmed));
                closeHandshake(RESULT_CANCELED);
                return;
            }
            handler.postDelayed(this, POLL_INTERVAL_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provisioning);

        messageView = findViewById(R.id.provisioning_message);
        hardwareEngine = new HardwareControlEngine(this);

        if (!isProvisioningHandshake(getIntent())) {
            // The activity was started manually instead of by the wizard. It
            // has no part in the enrollment, so the operator console takes
            // over and no result is reported.
            Log.w(TAG, "Started outside the provisioning wizard — redirecting to the console.");
            setResult(RESULT_CANCELED);
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        logHandshakeIntent(getIntent());

        if (isEnrolled()) {
            completeProvisioning();
            return;
        }

        showMessage(getString(R.string.provisioning_confirming));
        deadline = SystemClock.elapsedRealtime() + PROVISIONING_TIMEOUT_MS;
        handler.post(pollRunnable);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(pollRunnable);
        super.onDestroy();
    }

    /**
     * @return true when the intent originates from the provisioning wizard
     */
    private boolean isProvisioningHandshake(@Nullable Intent intent) {
        if (intent == null) {
            return false;
        }
        if (DevicePolicyManager.ACTION_PROVISION_MANAGED_DEVICE.equals(intent.getAction())) {
            return true;
        }
        return getRequestedAdminComponent(intent) != null;
    }

    /**
     * @return the administrator component the wizard asked for, or null
     */
    @Nullable
    @SuppressWarnings("deprecation")
    private ComponentName getRequestedAdminComponent(@Nullable Intent intent) {
        if (intent == null) {
            return null;
        }
        return intent.getParcelableExtra(
                DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME);
    }

    /**
     * Logs the parameters the wizard delivered and warns when they name a
     * different administrator than this application.
     */
    private void logHandshakeIntent(Intent intent) {
        ComponentName requested = getRequestedAdminComponent(intent);
        Log.i(TAG, "Provisioning handshake started. Requested admin: " + requested);

        if (requested != null && !EnterpriseSecurityReceiver.class.getName().equals(requested.getClassName())) {
            Log.w(TAG, "The wizard requested a different administrator: " + requested
                    + ". Continuing with this application.");
        }
    }

    /**
     * @return true when the application is an active administrator that holds
     *         the Device Owner or the Profile Owner role
     */
    private boolean isEnrolled() {
        if (!hardwareEngine.isAdminActive()) {
            return false;
        }
        return hardwareEngine.isDeviceOwner() || hardwareEngine.isProfileOwner();
    }

    /**
     * Applies the policy and closes the handshake successfully.
     */
    private void completeProvisioning() {
        if (handshakeClosed) {
            return;
        }
        handshakeClosed = true;
        handler.removeCallbacks(pollRunnable);

        Log.i(TAG, "Enrollment confirmed. Enforcing the privacy policy.");
        enforcePolicies();

        showMessage(getString(R.string.provisioning_complete));
        closeHandshake(RESULT_OK);
    }

    /**
     * Enforces the hardware privacy policy and prepares the kiosk
     * configuration. The lock-task session itself is not started here: only a
     * foreground activity can enter it, and the wizard still owns the screen.
     */
    private void enforcePolicies() {
        hardwareEngine.enableHardwarePrivacy();

        if (hardwareEngine.isDeviceOwner()) {
            hardwareEngine.enableTamperProofing();
        }
    }

    private void showMessage(String message) {
        if (messageView != null) {
            messageView.setText(message);
        }
    }

    private void closeHandshake(int resultCode) {
        setResult(resultCode);
        finish();
    }
}
