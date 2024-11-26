package net.qiujuer.library.clink.impl;

import net.qiujuer.library.clink.core.Scheduler;

import java.io.IOException;
import java.util.concurrent.*;

public class SchedulerImpl implements Scheduler {
    private final ScheduledExecutorService executorService;
    private final ExecutorService deliveryPool;

    public SchedulerImpl(int poolSize) {
        this.executorService = Executors.newScheduledThreadPool(poolSize,
                new NamedThreadFactory("Schedule-Thread-"));
        this.deliveryPool = Executors.newFixedThreadPool(1, new NamedThreadFactory("Delivery-Thread-"));
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable runnable, long delay, TimeUnit unit) {
        return executorService.schedule(runnable, delay, unit);
    }

    @Override
    public void delivery(Runnable runnable) {
        deliveryPool.execute(runnable);
    }

    @Override
    public void close() throws IOException {
        executorService.shutdownNow();
        deliveryPool.shutdownNow();
    }
}
