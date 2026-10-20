package com.kira_zina.haltprivacy.ui;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.kira_zina.haltprivacy.R;
import com.kira_zina.haltprivacy.security.HardwareControlEngine;

import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final long DURATION_15_MINUTES = 15L * 60L * 1000L;
    private static final long DURATION_30_MINUTES = 30L * 60L * 1000L;
    private static final long DURATION_1_HOUR = 60L * 60L * 1000L;

    private ProvisioningViewModel viewModel;
    private ActivityResultLauncher<Intent> provisioningLauncher;

    private Button btnDisable;
    private Button btnTamperOn;
    private Button btnTamperOff;

    private boolean kioskConfigured = false;
    private boolean timedLockoutActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this).get(ProvisioningViewModel.class);

        TextView privilegeLevel = findViewById(R.id.privilege_level);
        TextView lockdownStatus = findViewById(R.id.lockdown_status);
        TextView messageLog = findViewById(R.id.message_log);

        ImageView privilegeDot = findViewById(R.id.status_privilege_dot);
        ImageView lockdownDot = findViewById(R.id.status_lockdown_dot);
        ImageView kioskDot = findViewById(R.id.status_kiosk_dot);

        TextView kioskStatus = findViewById(R.id.kiosk_status);

        Button btnProvision = findViewById(R.id.btn_provision);
        Button btnEnable = findViewById(R.id.btn_enable);
        this.btnDisable = findViewById(R.id.btn_disable);
        this.btnTamperOn = findViewById(R.id.btn_tamper_on);
        this.btnTamperOff = findViewById(R.id.btn_tamper_off);

        EditText packageInput = findViewById(R.id.kiosk_package_input);
        Button btnAddPackage = findViewById(R.id.btn_add_package);
        ChipGroup packageChips = findViewById(R.id.kiosk_package_chips);
        TextView allowlistEmpty = findViewById(R.id.kiosk_allowlist_empty);

        MaterialButtonToggleGroup durationGroup = findViewById(R.id.duration_toggle_group);
        Button btnStartTimedLockout = findViewById(R.id.btn_start_timed_lockout);
        TextView countdown = findViewById(R.id.timed_lockout_countdown);

        if (privilegeLevel == null || lockdownStatus == null || messageLog == null ||
            btnProvision == null || btnEnable == null || btnDisable == null ||
            btnTamperOn == null || btnTamperOff == null || privilegeDot == null || lockdownDot == null ||
            kioskDot == null || kioskStatus == null || packageInput == null ||
            btnAddPackage == null || packageChips == null || allowlistEmpty == null ||
            durationGroup == null || btnStartTimedLockout == null || countdown == null) {
            throw new RuntimeException("Required UI elements not found in layout");
        }

        messageLog.setText(getString(R.string.logs_placeholder));

        viewModel.getPrivilegeLevel().observe(this, level -> {
            privilegeLevel.setText(getString(R.string.privilege_label, level));
            crossfadeDot(privilegeDot,
                "Device Owner".equals(level) ? R.drawable.status_dot_active : R.drawable.status_dot_inactive);
        });

        viewModel.getIsLockdownActive().observe(this, active -> {
            String status = active ? getString(R.string.active) : getString(R.string.inactive);
            lockdownStatus.setText(getString(R.string.lockdown_label, status));
            crossfadeDot(lockdownDot,
                active ? R.drawable.status_dot_active : R.drawable.status_dot_inactive);
        });

        viewModel.getIsKioskModeActive().observe(this, active -> {
            String status = active ? getString(R.string.locked) : getString(R.string.inactive);
            kioskStatus.setText(getString(R.string.kiosk_label, status));
            crossfadeDot(kioskDot,
                active ? R.drawable.status_dot_active : R.drawable.status_dot_inactive);
        });

        // The kiosk actions are only meaningful in one direction at a time.
        viewModel.getIsKioskModeConfigured().observe(this, configured -> {
            kioskConfigured = Boolean.TRUE.equals(configured);
            updateKioskButtons();
        });

        // A running timed lockout blocks every action that would remove the
        // policy, so the corresponding controls are greyed out.
        viewModel.getIsTimedLockoutActive().observe(this, active -> {
            timedLockoutActive = Boolean.TRUE.equals(active);
            updateKioskButtons();
        });

        viewModel.getRemainingTimeFormatted().observe(this, remaining -> {
            if (remaining == null || remaining.isEmpty()) {
                countdown.setText(R.string.timed_lockout_idle);
            } else {
                countdown.setText(getString(R.string.timed_lockout_countdown, remaining));
            }
        });

        // Render the secondary applications allowed to run inside the session.
        viewModel.getAllowedKioskPackages().observe(this, packages -> {
            renderAllowedPackages(packageChips, allowlistEmpty, packages);
        });

        // The ViewModel asks for a lock-task (kiosk) session change; only this
        // Activity can enter or leave lock-task mode.
        viewModel.getLockTaskCommand().observe(this, command -> {
            if (command == null) {
                return;
            }
            switch (command) {
                case ENTER:
                    HardwareControlEngine.startLockTaskSession(this);
                    break;
                case EXIT:
                    HardwareControlEngine.stopLockTaskSession(this);
                    break;
            }
            viewModel.onLockTaskCommandHandled(HardwareControlEngine.isLockTaskActive(this));
        });

        viewModel.getStatusMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                String currentText = messageLog.getText().toString();
                boolean isPlaceholder = currentText.equals(getString(R.string.logs_placeholder));
                messageLog.setText(isPlaceholder ? message : currentText + "\n" + message);
            }
        });

        // Register ActivityResultLauncher for provisioning intents
        provisioningLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> viewModel.onProvisioningResult(result.getResultCode() == RESULT_OK)
        );

        // Observe provisioning intents from the ViewModel and launch via the launcher
        viewModel.getProvisioningIntent().observe(this, intent -> {
            if (intent != null) {
                provisioningLauncher.launch(intent);
            }
        });

        btnProvision.setOnClickListener(v -> viewModel.attemptAutoProvisioning());
        btnEnable.setOnClickListener(v -> viewModel.enableLockdown());
        btnDisable.setOnClickListener(v -> viewModel.disableLockdown());
        btnTamperOn.setOnClickListener(v -> viewModel.enableKioskMode());
        btnTamperOff.setOnClickListener(v -> viewModel.disableKioskMode());

        btnAddPackage.setOnClickListener(v -> submitAllowedPackage(packageInput));
        packageInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submitAllowedPackage(packageInput);
                return true;
            }
            return false;
        });

        btnStartTimedLockout.setOnClickListener(v ->
                viewModel.startTimedLockout(selectedDurationMillis(durationGroup)));

        if (savedInstanceState == null) {
            applyEntryFade();
        }
    }

    /**
     * @return the duration selected in the preset toggle group
     */
    private long selectedDurationMillis(MaterialButtonToggleGroup durationGroup) {
        int checkedId = durationGroup.getCheckedButtonId();
        if (checkedId == R.id.btn_duration_30m) {
            return DURATION_30_MINUTES;
        }
        if (checkedId == R.id.btn_duration_1h) {
            return DURATION_1_HOUR;
        }
        return DURATION_15_MINUTES;
    }

    /**
     * Keeps the lockdown and kiosk controls in sync with the current state.
     * While a timed lockout runs, nothing that would remove the policy can be
     * triggered from the UI.
     */
    private void updateKioskButtons() {
        btnTamperOn.setEnabled(!kioskConfigured && !timedLockoutActive);
        btnTamperOff.setEnabled(kioskConfigured && !timedLockoutActive);
        btnDisable.setEnabled(!timedLockoutActive);
    }

    /**
     * Validates and forwards the typed package name to the ViewModel. The
     * input field is only cleared when the package was accepted.
     */
    private void submitAllowedPackage(EditText packageInput) {
        String value = packageInput.getText() == null ? "" : packageInput.getText().toString().trim();
        if (viewModel.addAllowedPackage(value)) {
            packageInput.setText("");
            packageInput.clearFocus();
            hideKeyboard(packageInput);
        }
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**
     * Rebuilds the chip list from the current allowlist. Every chip carries a
     * close icon that removes the package again.
     */
    private void renderAllowedPackages(ChipGroup chipGroup, TextView emptyView, Set<String> packages) {
        chipGroup.removeAllViews();

        if (packages == null || packages.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            return;
        }

        emptyView.setVisibility(View.GONE);
        for (String packageName : packages) {
            Chip chip = new Chip(this);
            chip.setText(packageName);
            chip.setCloseIconVisible(true);
            chip.setEnsureMinTouchTargetSize(false);
            chip.setOnCloseIconClickListener(v -> viewModel.removeAllowedPackage(packageName));
            chipGroup.addView(chip);
        }
    }

    /**
     * One-shot entry animation.
     *
     * <p>Previously the fade was re-triggered from {@code onWindowFocusChanged},
     * so it replayed on every focus change (keyboard toggles, dialog dismissal,
     * returning from the provisioning wizard). It is now started exactly once,
     * when the activity is first created.</p>
     */
    private void applyEntryFade() {
        View root = findViewById(android.R.id.content);
        if (root == null) {
            return;
        }
        root.setAlpha(0f);
        root.post(() -> root.animate().alpha(1f).setDuration(600).start());
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.refreshStatus();
        viewModel.applyManagedConfigurations();
        viewModel.updateKioskState(HardwareControlEngine.isLockTaskActive(this));
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // The lock-task session can be left by the platform (for example
            // through the keyguard), so the indicator is re-synced here.
            viewModel.updateKioskState(HardwareControlEngine.isLockTaskActive(this));
        }
    }

    private void crossfadeDot(ImageView dot, int newDrawableRes) {
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(dot, "alpha", 1f, 0f).setDuration(250);
        fadeOut.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                dot.setImageResource(newDrawableRes);
                dot.animate().alpha(1f).setDuration(300).start();
            }
        });
        fadeOut.start();
    }
}
