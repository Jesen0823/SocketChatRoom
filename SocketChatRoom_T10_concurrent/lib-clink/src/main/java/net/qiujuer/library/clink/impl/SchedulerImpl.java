package net.qiujuer.library.clink.impl;

import net.qiujuer.library.clink.core.Scheduler;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SchedulerImpl implements Scheduler {
    private final ScheduledExecutorService executorService;

    public SchedulerImpl(int poolSize) {
        executorService = Executors.newScheduledThreadPool(poolSize,
                new IoSelectorProvider.IoProviderThreadFactory("Schedule-Thread"));
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable runnable, long delay, TimeUnit unit) {
        return executorService.schedule(runnable,delay,unit);
    }

    @Override
    public void close() throws IOException {
        executorService.shutdownNow();
    }
}
