package com.projeto.egoodapp.security;

import java.util.Objects;

/** Uses monotonic time within a boot, wall time across boots; rollback expires safely. */
public final class SessionDeadline {
    public static final long TIMEOUT_MS = 6 * 60 * 60 * 1000L;
    public String userId;
    public long lastWall, lastElapsed;
    public int bootCount;

    public void touch(String uid, long wall, long elapsed, int boot) {
        userId = uid; lastWall = wall; lastElapsed = elapsed; bootCount = boot;
    }
    public long remaining(String uid, long wall, long elapsed, int boot) {
        if (uid == null || !Objects.equals(uid, userId)) return 0;
        long idle = boot == bootCount ? elapsed - lastElapsed : wall - lastWall;
        if (idle < 0) return 0;
        return Math.max(0, TIMEOUT_MS - idle);
    }
}
