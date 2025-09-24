package org.jesen.library.clink.core;

import java.io.Closeable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 任务调度器
 */
public interface Scheduler extends Closeable {
    /**
     * 调度延迟任务
     */
    ScheduledFuture<?> schedule(Runnable runnable, long delay, TimeUnit unit);

    void delivery(Runnable runnable);
}
