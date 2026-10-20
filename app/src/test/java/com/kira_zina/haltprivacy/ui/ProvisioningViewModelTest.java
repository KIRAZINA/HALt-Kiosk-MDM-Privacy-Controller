package com.kira_zina.haltprivacy.ui;

import android.app.Application;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.kira_zina.haltprivacy.provisioning.ProvisioningEngine;
import com.kira_zina.haltprivacy.security.HardwareControlEngine;
import com.kira_zina.haltprivacy.security.PrefsManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the strict timed lockout: the deadline is calculated and persisted,
 * the policies are enforced, and the disable actions are refused while the
 * lockout is running.
 */
@RunWith(MockitoJUnitRunner.class)
public class ProvisioningViewModelTest {

    private static final long DURATION = 15L * 60L * 1000L;

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock private Application mockApplication;
    @Mock private ProvisioningEngine mockProvisioningEngine;
    @Mock private HardwareControlEngine mockHardwareEngine;
    @Mock private PrefsManager mockPrefsManager;

    private ProvisioningViewModel viewModel;

    @Before
    public void setUp() {
        when(mockPrefsManager.getAllowedKioskPackages()).thenReturn(new LinkedHashSet<>());
        when(mockPrefsManager.isLockdownActive()).thenReturn(false);
        when(mockPrefsManager.isKioskModeConfigured()).thenReturn(false);
        when(mockPrefsManager.isTimedLockoutActive()).thenReturn(false);
        when(mockHardwareEngine.getAllowedKioskPackages()).thenReturn(new LinkedHashSet<>());
        when(mockHardwareEngine.enableTamperProofing(anySet())).thenReturn(true);
        when(mockHardwareEngine.disableTamperProofing()).thenReturn(true);

        viewModel = createViewModel();
    }

    /**
     * Builds the ViewModel and clears the invocations that its constructor
     * performs, so that a test only observes what it triggers itself.
     */
    private ProvisioningViewModel createViewModel() {
        ProvisioningViewModel created = new ProvisioningViewModel(mockApplication,
                mockProvisioningEngine, mockHardwareEngine, mockPrefsManager);
        Mockito.clearInvocations(mockPrefsManager, mockHardwareEngine, mockProvisioningEngine);
        return created;
    }

    @Test
    public void startTimedLockout_storesTheEndTimestamp() {
        when(mockHardwareEngine.isDeviceOwner()).thenReturn(true);

        long before = System.currentTimeMillis();
        viewModel.startTimedLockout(DURATION);
        long after = System.currentTimeMillis();

        verify(mockPrefsManager).setLockoutEndTimestamp(anyLong());
        // The stored deadline has to lie inside the requested window.
        org.mockito.ArgumentCaptor<Long> captor = org.mockito.ArgumentCaptor.forClass(Long.class);
        verify(mockPrefsManager).setLockoutEndTimestamp(captor.capture());
        assertTrue(captor.getValue() >= before + DURATION);
        assertTrue(captor.getValue() <= after + DURATION);
    }

    @Test
    public void startTimedLockout_appliesThePolicies() {
        when(mockHardwareEngine.isDeviceOwner()).thenReturn(true);

        viewModel.startTimedLockout(DURATION);

        verify(mockHardwareEngine).enableHardwarePrivacy();
        verify(mockHardwareEngine).enableTamperProofing(anySet());
        verify(mockPrefsManager).setKioskModeConfigured(true);
    }

    @Test
    public void startTimedLockout_marksTheLockoutActive() {
        when(mockHardwareEngine.isDeviceOwner()).thenReturn(true);

        viewModel.startTimedLockout(DURATION);

        assertTrue(viewModel.getIsTimedLockoutActive().getValue());
        assertNotNull(viewModel.getRemainingTimeFormatted().getValue());
        assertFalse(viewModel.getRemainingTimeFormatted().getValue().isEmpty());
    }

    @Test
    public void startTimedLockout_withoutDeviceOwner_isRefused() {
        when(mockHardwareEngine.isDeviceOwner()).thenReturn(false);

        viewModel.startTimedLockout(DURATION);

        verify(mockPrefsManager, never()).setLockoutEndTimestamp(anyLong());
        verify(mockHardwareEngine, never()).enableHardwarePrivacy();
    }

    @Test
    public void startTimedLockout_withInvalidDuration_isRefused() {
        viewModel.startTimedLockout(0L);

        verify(mockPrefsManager, never()).setLockoutEndTimestamp(anyLong());
        verify(mockHardwareEngine, never()).enableHardwarePrivacy();
    }

    @Test
    public void disableKioskMode_isBlockedWhileTheLockoutIsRunning() {
        when(mockHardwareEngine.isDeviceOwner()).thenReturn(true);
        when(mockPrefsManager.isTimedLockoutActive()).thenReturn(true);
        when(mockPrefsManager.getLockoutEndTimestamp())
                .thenReturn(System.currentTimeMillis() + DURATION);

        viewModel = createViewModel();
        viewModel.startTimedLockout(DURATION);

        viewModel.disableKioskMode();

        verify(mockHardwareEngine, never()).disableTamperProofing();
        assertTrue(viewModel.getStatusMessage().getValue()
                .contains("Timed lockout active"));
    }

    @Test
    public void disableLockdown_isBlockedWhileTheLockoutIsRunning() {
        when(mockHardwareEngine.isDeviceOwner()).thenReturn(true);
        when(mockPrefsManager.isTimedLockoutActive()).thenReturn(true);
        when(mockPrefsManager.getLockoutEndTimestamp())
                .thenReturn(System.currentTimeMillis() + DURATION);

        viewModel = createViewModel();
        viewModel.startTimedLockout(DURATION);

        viewModel.disableLockdown();

        verify(mockHardwareEngine, never()).disableHardwarePrivacy();
        assertTrue(viewModel.getStatusMessage().getValue()
                .contains("Timed lockout active"));
    }

    @Test
    public void disableKioskMode_succeedsWhenTheDeadlineHasPassed() {
        when(mockHardwareEngine.isDeviceOwner()).thenReturn(true);
        when(mockPrefsManager.isTimedLockoutActive()).thenReturn(false);
        when(mockPrefsManager.getLockoutEndTimestamp())
                .thenReturn(System.currentTimeMillis() - 1_000L);

        viewModel = createViewModel();

        viewModel.disableKioskMode();

        verify(mockHardwareEngine).disableTamperProofing();
    }

    @Test
    public void disableKioskMode_succeedsWhenNoLockoutIsStored() {
        when(mockHardwareEngine.isDeviceOwner()).thenReturn(true);
        when(mockPrefsManager.isTimedLockoutActive()).thenReturn(false);
        when(mockPrefsManager.getLockoutEndTimestamp()).thenReturn(0L);

        viewModel = createViewModel();

        viewModel.disableKioskMode();

        verify(mockHardwareEngine).disableTamperProofing();
    }

    @Test
    public void isTimedLockoutBlocked_reflectsTheActiveState() {
        assertFalse(viewModel.isTimedLockoutBlocked());
    }

    @Test
    public void refreshStatus_clearsAnExpiredLockout() {
        when(mockPrefsManager.isTimedLockoutActive()).thenReturn(false);
        when(mockPrefsManager.getLockoutEndTimestamp()).thenReturn(1_000L);

        viewModel.refreshStatus();

        verify(mockPrefsManager).clearLockoutEndTimestamp();
        assertFalse(viewModel.getIsTimedLockoutActive().getValue());
    }

    @Test
    public void addAllowedPackage_rejectsInvalidNames() {
        assertFalse(viewModel.addAllowedPackage("not a package"));
        assertFalse(viewModel.addAllowedPackage(""));
        assertFalse(viewModel.addAllowedPackage(null));

        verify(mockPrefsManager, never()).setAllowedKioskPackages(anySet());
    }

    @Test
    public void addAllowedPackage_storesAValidName() {
        assertTrue(viewModel.addAllowedPackage("com.android.chrome"));

        verify(mockPrefsManager).setAllowedKioskPackages(anySet());
        assertTrue(viewModel.currentAllowedKioskPackages().contains("com.android.chrome"));
    }

    @Test
    public void removeAllowedPackage_removesTheName() {
        viewModel.addAllowedPackage("com.android.chrome");

        assertTrue(viewModel.removeAllowedPackage("com.android.chrome"));

        assertFalse(viewModel.currentAllowedKioskPackages().contains("com.android.chrome"));
    }
}
