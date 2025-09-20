package org.jesen.library.clink.core;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public abstract class SchedulerJob implements Runnable {
    // 空闲时间
    protected final long idleTimeoutMilliseconds;
    protected final Connector connector;
    private volatile Scheduler scheduler;
    private volatile ScheduledFuture scheduledFuture;

    protected SchedulerJob(long idleTimeOut, TimeUnit unit, Connector connector) {
        this.idleTimeoutMilliseconds = unit.toMillis(idleTimeOut);
        this.connector = connector;
    }

    synchronized void schedule(Scheduler scheduler) {
        this.scheduler = scheduler;
        schedule(idleTimeoutMilliseconds);
    }

    protected synchronized void schedule(long timeoutMilliseconds) {
        if (scheduler != null) {
            scheduledFuture = scheduler.schedule(this, timeoutMilliseconds, TimeUnit.MILLISECONDS);
        }
    }

    void unSchedule() {
        if (scheduler != null) {
            scheduler = null;
        }
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
            scheduledFuture = null;
        }
    }
}
