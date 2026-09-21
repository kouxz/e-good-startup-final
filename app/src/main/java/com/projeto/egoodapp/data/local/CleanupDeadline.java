package com.projeto.egoodapp.data.local;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Bounds an optional provider cleanup and ignores duplicate or late responses. */
final class CleanupDeadline {
    interface Scheduler {
        void schedule(Runnable timeout);
        void cancel(Runnable timeout);
    }

    static void run(Consumer<Runnable> cleanup, Scheduler scheduler,
                    Runnable cancelRequest, Runnable completed) {
        AtomicBoolean finished = new AtomicBoolean();
        Runnable[] timeout = new Runnable[1];
        Consumer<Boolean> finish = expired -> {
            if (!finished.compareAndSet(false, true)) return;
            scheduler.cancel(timeout[0]);
            try {
                if (expired) cancelRequest.run();
            } catch (RuntimeException cancellationFailure) {
                // The Firebase session is already closed; provider cleanup is optional.
            } finally {
                completed.run();
            }
        };
        timeout[0] = () -> finish.accept(true);
        scheduler.schedule(timeout[0]);
        try {
            cleanup.accept(() -> finish.accept(false));
        } catch (RuntimeException unavailableProvider) {
            finish.accept(false);
        }
    }

    private CleanupDeadline() {}
}
