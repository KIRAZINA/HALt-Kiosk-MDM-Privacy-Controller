package com.kira_zina.haltprivacy.provisioning;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.kira_zina.haltprivacy.R;

/**
 * Activity for QR/NFC Android Enterprise enrollment.
 * Handles the provisioning UI and enrollment flow.
 */
public class ProvisioningActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provisioning);
    }
}