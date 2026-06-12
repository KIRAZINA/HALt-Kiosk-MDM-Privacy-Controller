package com.kira_zina.haltprivacy.ui;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.kira_zina.haltprivacy.R;

public class MainActivity extends AppCompatActivity {

    private ProvisioningViewModel viewModel;
    private ActivityResultLauncher<Intent> provisioningLauncher;

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

        Button btnProvision = findViewById(R.id.btn_provision);
        Button btnEnable = findViewById(R.id.btn_enable);
        Button btnDisable = findViewById(R.id.btn_disable);
        Button btnTamperOn = findViewById(R.id.btn_tamper_on);
        Button btnTamperOff = findViewById(R.id.btn_tamper_off);

        if (privilegeLevel == null || lockdownStatus == null || messageLog == null ||
            btnProvision == null || btnEnable == null || btnDisable == null ||
            btnTamperOn == null || btnTamperOff == null || privilegeDot == null || lockdownDot == null) {
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
        btnTamperOn.setOnClickListener(v -> viewModel.enableTamperProofing());
        btnTamperOff.setOnClickListener(v -> viewModel.disableTamperProofing());
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            View root = findViewById(android.R.id.content);
            root.setAlpha(0f);
            root.animate().alpha(1f).setDuration(600).start();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.refreshStatus();
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
