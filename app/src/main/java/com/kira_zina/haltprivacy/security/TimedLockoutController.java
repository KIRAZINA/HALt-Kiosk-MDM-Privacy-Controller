package com.kira_zina.haltprivacy.security;

import java.util.Locale;

/**
 * Owns the state of a strict timed lockout.
 *
 * <p>A timed lockout is a deadline that survives a reboot: the end timestamp
 * is persisted, and while it lies in the future the policy may not be
 * disabled. This class holds the pure decision logic — deadline arithmetic,
 * the remaining time and its formatting — so that it can be verified without
 * an Android runtime.</p>
 */
public class TimedLockoutController {

    private final PrefsManager prefsManager;

    public TimedLockoutController(PrefsManager prefsManager) {
        this.prefsManager = prefsManager;
    }

    /**
     * Computes the deadline of a new timed lockout and stores it.
     *
     * @param durationMillis how long the lockout should last; negative values
     *                       are treated as zero
     * @param now            the current wall-clock time in epoch milliseconds
     * @return the stored end timestamp
     */
    public long start(long durationMillis, long now) {
        long endTimestamp = now + Math.max(0L, durationMillis);
        prefsManager.setLockoutEndTimestamp(endTimestamp);
        return endTimestamp;
    }

    /**
     * @return the wall-clock time (epoch milliseconds) at which the lockout
     *         expires, or 0 when no lockout is configured
     */
    public long endTimestamp() {
        return prefsManager.getLockoutEndTimestamp();
    }

    /**
     * @return true while the lockout is still running
     */
    public boolean isActive() {
        return prefsManager.isTimedLockoutActive();
    }

    /**
     * @param now the current wall-clock time in epoch milliseconds
     * @return true while the stored deadline lies in the future
     */
    public boolean isActiveAt(long now) {
        long endTimestamp = endTimestamp();
        return endTimestamp > 0L && now < endTimestamp;
    }

    /**
     * @return the milliseconds left before the lockout expires; 0 when it is
     *         not running
     */
    public long remainingMillis() {
        return remainingMillisAt(System.currentTimeMillis());
    }

    /**
     * @param now the current wall-clock time in epoch milliseconds
     * @return the milliseconds left before the lockout expires; 0 when it is
     *         not running
     */
    public long remainingMillisAt(long now) {
        long remaining = endTimestamp() - now;
        return remaining > 0L ? remaining : 0L;
    }

    /**
     * Removes the deadline, which makes the lockout inactive.
     */
    public void clear() {
        prefsManager.clearLockoutEndTimestamp();
    }

    /**
     * Formats a remaining duration for display.
     *
     * @param remainingMillis the milliseconds left
     * @return {@code "01h 05m 30s"} for long durations, {@code "05m 30s"} for
     *         shorter ones and {@code "00m 00s"} when nothing is left
     */
    public static String formatRemaining(long remainingMillis) {
        if (remainingMillis <= 0L) {
            return "00m 00s";
        }

        long totalSeconds = (remainingMillis + 999L) / 1000L;
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;

        if (hours > 0L) {
            return String.format(Locale.US, "%02dh %02dm %02ds", hours, minutes, seconds);
        }
        return String.format(Locale.US, "%02dm %02ds", minutes, seconds);
    }

    /**
     * Formats a remaining duration as whole minutes, rounded up.
     *
     * @param remainingMillis the milliseconds left
     * @return the number of minutes left, never negative
     */
    public static long remainingMinutes(long remainingMillis) {
        if (remainingMillis <= 0L) {
            return 0L;
        }
        return (remainingMillis + 59_999L) / 60_000L;
    }
}
