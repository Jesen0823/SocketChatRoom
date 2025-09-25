package org.jesen.library.clink.core;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public abstract class ScheduleJob implements Runnable {
    protected final long idleTimeoutMilliseconds;
    protected final Connector connector;
    private volatile Scheduler scheduler;
    private volatile ScheduledFuture<?> future;

    protected ScheduleJob(long idleTime, TimeUnit unit, Connector connector) {
        this.idleTimeoutMilliseconds = unit.toMillis(idleTime);
        this.connector = connector;
    }

    synchronized void schedule(Scheduler scheduler) {
        this.scheduler = scheduler;
        realSchedule(idleTimeoutMilliseconds);
    }

    synchronized void unSchedule() {
        if (scheduler != null) {
            scheduler = null;
        }
        if (future != null) {
            future.cancel(true);
            future = null;
        }
    }

    protected synchronized void realSchedule(long timeoutMilliseconds) {
        if (scheduler != null) {
            future = scheduler.schedule(this, timeoutMilliseconds, TimeUnit.MILLISECONDS);
        }
    }
}
