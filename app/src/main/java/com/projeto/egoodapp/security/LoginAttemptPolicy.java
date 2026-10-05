package com.projeto.egoodapp.security;

/** Device-wide client cooldown, complementary to Firebase's server abuse controls. */
public final class LoginAttemptPolicy {
    public static final int LIMIT = 5;
    public static final long COOLDOWN_MS = 60_000, SERVER_COOLDOWN_MS = 300_000;
    public int failures;
    public long blockedUntil, observedAt, blockDuration;

    public long remaining(long now) {
        if (now < observedAt && blockedUntil > observedAt) blockedUntil = now + blockDuration;
        observedAt = now;
        if (blockedUntil <= now) { blockedUntil = 0; blockDuration = 0; }
        return Math.max(0, blockedUntil - now);
    }
    public boolean failure(long now, boolean invalidCredentials, boolean serverThrottled) {
        if (remaining(now) > 0) return false;
        if (serverThrottled) return block(now, SERVER_COOLDOWN_MS);
        if (invalidCredentials && ++failures >= LIMIT) return block(now, COOLDOWN_MS);
        return false;
    }
    private boolean block(long now, long duration) {
        failures = 0; blockDuration = duration; blockedUntil = now + duration; return true;
    }
    public void success() { failures = 0; blockedUntil = 0; blockDuration = 0; observedAt = 0; }
}
