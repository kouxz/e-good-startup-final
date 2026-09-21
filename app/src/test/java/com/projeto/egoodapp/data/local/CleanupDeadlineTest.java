package com.projeto.egoodapp.data.local;

import org.junit.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class CleanupDeadlineTest {
    private static class Clock implements CleanupDeadline.Scheduler {
        Runnable timeout;
        @Override public void schedule(Runnable timeout) { this.timeout = timeout; }
        @Override public void cancel(Runnable timeout) {
            if (this.timeout == timeout) this.timeout = null;
        }
        void expire() { assertNotNull(timeout); timeout.run(); }
    }

    @Test public void stalledProviderReleasesLoginAndLateCallbackCannotCompleteAgain() {
        Clock clock = new Clock();
        AtomicInteger completions = new AtomicInteger();
        AtomicInteger cancellations = new AtomicInteger();
        Runnable[] providerReply = new Runnable[1];
        CleanupDeadline.run(done -> providerReply[0] = done, clock,
                cancellations::incrementAndGet, completions::incrementAndGet);
        assertEquals(0, completions.get());
        clock.expire();
        assertEquals(1, completions.get());
        assertEquals(1, cancellations.get());
        assertNull(clock.timeout);
        providerReply[0].run();
        assertEquals(1, completions.get());
    }

    @Test public void providerReplyCompletesImmediatelyAndRemovesDeadline() {
        Clock clock = new Clock();
        AtomicInteger completions = new AtomicInteger();
        AtomicInteger cancellations = new AtomicInteger();
        CleanupDeadline.run(done -> { done.run(); done.run(); }, clock,
                cancellations::incrementAndGet, completions::incrementAndGet);
        assertEquals(1, completions.get());
        assertEquals(0, cancellations.get());
        assertNull(clock.timeout);
    }

    @Test public void unavailableProviderStillReleasesLogin() {
        Clock clock = new Clock();
        AtomicInteger completions = new AtomicInteger();
        CleanupDeadline.run(done -> { throw new IllegalStateException("Unavailable"); }, clock,
                () -> fail("No pending request"), completions::incrementAndGet);
        assertEquals(1, completions.get());
        assertNull(clock.timeout);
    }

    @Test public void cancellationCallbackAndCancellationFailureCannotSuppressCompletion() {
        Clock clock = new Clock();
        AtomicInteger completions = new AtomicInteger();
        Runnable[] providerReply = new Runnable[1];
        CleanupDeadline.run(done -> providerReply[0] = done, clock, () -> {
            providerReply[0].run();
            throw new IllegalStateException("Provider cancellation failed");
        }, completions::incrementAndGet);
        clock.expire();
        assertEquals(1, completions.get());
        providerReply[0].run();
        assertEquals(1, completions.get());
    }
}
