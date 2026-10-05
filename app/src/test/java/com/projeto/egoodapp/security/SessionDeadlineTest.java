package com.projeto.egoodapp.security;

import com.google.gson.Gson;
import org.junit.Test;
import static org.junit.Assert.*;

public class SessionDeadlineTest {
    @Test public void sixHourBoundaryAndInteractionRenewal() {
        SessionDeadline session = new SessionDeadline(); session.touch("uid", 10000, 1000, 1);
        assertEquals(1000, session.remaining("uid", 999999, 1000 + SessionDeadline.TIMEOUT_MS - 1000, 1));
        assertEquals(0, session.remaining("uid", 999999, 1000 + SessionDeadline.TIMEOUT_MS, 1));
        session.touch("uid", 30000, 5000, 1);
        assertEquals(SessionDeadline.TIMEOUT_MS, session.remaining("uid", 30000, 5000, 1));
    }
    @Test public void rotationAndProcessRestoreDoNotTouchDeadline() {
        SessionDeadline session = new SessionDeadline(); session.touch("uid", 10000, 1000, 1);
        Gson gson = new Gson(); session = gson.fromJson(gson.toJson(session), SessionDeadline.class);
        assertEquals(SessionDeadline.TIMEOUT_MS - 2000, session.remaining("uid", 12000, 3000, 1));
        assertEquals(1000, session.lastElapsed);
    }
    @Test public void monotonicClockIgnoresWallChangesAndBootUsesPersistedWall() {
        SessionDeadline session = new SessionDeadline(); session.touch("uid", 10000, 1000, 1);
        assertEquals(SessionDeadline.TIMEOUT_MS - 1000, session.remaining("uid", 1, 2000, 1));
        assertEquals(0, session.remaining("uid", 10000 + SessionDeadline.TIMEOUT_MS, 20, 2));
        assertEquals(0, session.remaining("uid", 9999, 20, 2));
    }
    @Test public void changingUidNeverInheritsAnotherSession() {
        SessionDeadline session = new SessionDeadline(); session.touch("uid", 10000, 1000, 1);
        assertEquals(0, session.remaining("other", 10000, 1000, 1));
        assertEquals(0, session.remaining(null, 10000, 1000, 1));
    }
}
