package com.KIRA_ZINA.haltprivacy.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.KIRA_ZINA.haltprivacy.R;

public class MainActivity extends AppCompatActivity {

    private ProvisioningViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this).get(ProvisioningViewModel.class);

        TextView privilegeLevel = findViewById(R.id.privilege_level);
        TextView lockdownStatus = findViewById(R.id.lockdown_status);
        TextView messageLog = findViewById(R.id.message_log);

        Button btnProvision = findViewById(R.id.btn_provision);
        Button btnEnable = findViewById(R.id.btn_enable);
        Button btnDisable = findViewById(R.id.btn_disable);
        Button btnTamperOn = findViewById(R.id.btn_tamper_on);
        Button btnTamperOff = findViewById(R.id.btn_tamper_off);

        // Add null safety checks
        if (privilegeLevel == null || lockdownStatus == null || messageLog == null ||
            btnProvision == null || btnEnable == null || btnDisable == null ||
            btnTamperOn == null || btnTamperOff == null) {
            throw new RuntimeException("Required UI elements not found in layout");
        }

        // Initialize message log with placeholder text
        messageLog.setText(getString(R.string.logs_placeholder));

        viewModel.getPrivilegeLevel().observe(this, level ->
            privilegeLevel.setText(getString(R.string.privilege_label, level)));

        viewModel.getIsLockdownActive().observe(this, active -> {
            String status = active ? getString(R.string.active) : getString(R.string.inactive);
            lockdownStatus.setText(getString(R.string.lockdown_label, status));
        });

        viewModel.getStatusMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                String currentText = messageLog.getText().toString();
                messageLog.setText(currentText + "\n" + message);
            }
        });

        btnProvision.setOnClickListener(v -> viewModel.attemptAutoProvisioning());
        btnEnable.setOnClickListener(v -> viewModel.enableLockdown());
        btnDisable.setOnClickListener(v -> viewModel.disableLockdown());
        btnTamperOn.setOnClickListener(v -> viewModel.enableTamperProofing());
        btnTamperOff.setOnClickListener(v -> viewModel.disableTamperProofing());
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.refreshStatus();
    }
}