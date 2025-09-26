package org.jesen.library.clink.impl.stealing;

import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.IntFunction;

/**
 * 窃取调度任务
 * 空闲任务给繁忙任务帮忙
 */
public class StealingService {

    // 安全阈值，当队列数量低于该值时，不可窃取
    private final int minSafeThreshold;
    private final StealingSelectorThread[] threads;

    // 任务队列
    private final LinkedBlockingQueue<IoTask>[] queues;
    // 结束标记
    private volatile boolean isTerminated = false;

    public StealingService(int minSafeThreshold, StealingSelectorThread[] threads) {
        this.minSafeThreshold = minSafeThreshold;
        this.threads = threads;
        this.queues = Arrays.stream(threads)
                .map(StealingSelectorThread::getReadyTaskQueue)
                .toArray((IntFunction<LinkedBlockingQueue<IoTask>[]>) LinkedBlockingQueue[]::new);
    }

    /**
     * 窃取一个任务，排除自己，从他人队列窃取一个任务
     *
     * @param excludedQueue 待排除的队列
     * @return 窃取成功返回实例，失败返回NULL
     */
    IoTask steal(final LinkedBlockingQueue<IoTask> excludedQueue) {
        final int minSafeThreshold = this.minSafeThreshold;
        final LinkedBlockingQueue<IoTask>[] queues = this.queues;
        for (LinkedBlockingQueue<IoTask> queue : queues) {
            if (queue == excludedQueue) {
                continue;
            }
            int size = queue.size();
            if (size > minSafeThreshold) {
                IoTask poll = queue.poll();
                if (poll != null) return poll;
            }
        }
        return null;
    }

    /**
     * 获取一个不繁忙线程
     */
    public StealingSelectorThread getNotBusyThread() {
        StealingSelectorThread targetThread = null;
        long targetKeyCount = Long.MAX_VALUE;
        for (StealingSelectorThread thread : threads) {
            long registerKeyCount = thread.getSaturatingCapacity();
            if (registerKeyCount != -1 && registerKeyCount < targetKeyCount) {
                targetKeyCount = registerKeyCount;
                targetThread = thread;
            }
        }
        return targetThread;
    }

    /**
     * 是否已结束
     */
    public boolean isTerminated() {
        return isTerminated;
    }

    /**
     * 执行任务
     */
    public void execute(IoTask task) {

    }

    /**
     * 结束操作
     */
    public void shutdown() {
        if (isTerminated) {
            return;
        }
        isTerminated = true;
        for (StealingSelectorThread thread : threads) {
            thread.exit();
        }
    }
}
