package com.kira_zina.haltprivacy.security;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TimedLockoutControllerTest {

    private static final long NOW = 1_700_000_000_000L;

    @Mock private PrefsManager prefsManager;

    private TimedLockoutController controller;

    @Before
    public void setUp() {
        controller = new TimedLockoutController(prefsManager);
    }

    @Test
    public void start_computesAndStoresTheEndTimestamp() {
        long end = controller.start(900_000L, NOW);

        assertEquals(NOW + 900_000L, end);
        verify(prefsManager).setLockoutEndTimestamp(NOW + 900_000L);
    }

    @Test
    public void start_withNegativeDuration_storesTheCurrentTime() {
        long end = controller.start(-5_000L, NOW);

        assertEquals(NOW, end);
        verify(prefsManager).setLockoutEndTimestamp(NOW);
    }

    @Test
    public void isActiveAt_isTrueBeforeTheDeadline() {
        when(prefsManager.getLockoutEndTimestamp()).thenReturn(NOW + 1_000L);

        assertTrue(controller.isActiveAt(NOW));
    }

    @Test
    public void isActiveAt_isFalseAfterTheDeadline() {
        when(prefsManager.getLockoutEndTimestamp()).thenReturn(NOW - 1L);

        assertFalse(controller.isActiveAt(NOW));
    }

    @Test
    public void isActiveAt_isFalseWithoutAStoredDeadline() {
        when(prefsManager.getLockoutEndTimestamp()).thenReturn(0L);

        assertFalse(controller.isActiveAt(NOW));
    }

    @Test
    public void isActive_delegatesToThePreferences() {
        when(prefsManager.isTimedLockoutActive()).thenReturn(true);

        assertTrue(controller.isActive());
    }

    @Test
    public void remainingMillisAt_isZeroWhenExpired() {
        when(prefsManager.getLockoutEndTimestamp()).thenReturn(NOW - 5_000L);

        assertEquals(0L, controller.remainingMillisAt(NOW));
    }

    @Test
    public void remainingMillisAt_isTheDifferenceToTheDeadline() {
        when(prefsManager.getLockoutEndTimestamp()).thenReturn(NOW + 65_000L);

        assertEquals(65_000L, controller.remainingMillisAt(NOW));
    }

    @Test
    public void clear_removesTheStoredDeadline() {
        controller.clear();

        verify(prefsManager).clearLockoutEndTimestamp();
    }

    @Test
    public void formatRemaining_formatsMinutesAndSeconds() {
        assertEquals("05m 30s", TimedLockoutController.formatRemaining(330_000L));
    }

    @Test
    public void formatRemaining_formatsHoursWhenPresent() {
        assertEquals("01h 05m 30s", TimedLockoutController.formatRemaining(3_930_000L));
    }

    @Test
    public void formatRemaining_isZeroWhenNothingIsLeft() {
        assertEquals("00m 00s", TimedLockoutController.formatRemaining(0L));
        assertEquals("00m 00s", TimedLockoutController.formatRemaining(-10L));
    }

    @Test
    public void formatRemaining_roundsPartialSecondsUp() {
        assertEquals("00m 01s", TimedLockoutController.formatRemaining(1L));
    }

    @Test
    public void remainingMinutes_roundsUp() {
        assertEquals(1L, TimedLockoutController.remainingMinutes(1L));
        assertEquals(2L, TimedLockoutController.remainingMinutes(60_001L));
        assertEquals(0L, TimedLockoutController.remainingMinutes(0L));
    }
}
