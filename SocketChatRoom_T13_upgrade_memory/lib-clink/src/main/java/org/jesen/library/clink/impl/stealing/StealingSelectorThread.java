package org.jesen.library.clink.impl.stealing;

import org.jesen.library.clink.core.IoTask;
import org.jesen.library.clink.utils.CloseUtils;

import java.io.IOException;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 可窃取任务的线程
 */
public abstract class StealingSelectorThread extends Thread {
    private static final int ONCE_READ_TASK_MAX = 128;
    private static final int ONCE_WRITE_TASK_MAX = 128;
    // 单次就绪的总任务数量
    private static final int ONCE_RUN_TASK_MAX = ONCE_READ_TASK_MAX + ONCE_WRITE_TASK_MAX;

    // 允许的操作
    private static final int VALID_OPS = SelectionKey.OP_READ | SelectionKey.OP_WRITE;
    private final Selector selector;
    private volatile boolean isRunning = true;
    // 当前Selector中已就绪的任务队列
    private final ArrayBlockingQueue<IoTask> mReadyTaskQueue = new ArrayBlockingQueue<>(ONCE_RUN_TASK_MAX);
    // 暂存待注册任务队列，条件允许时将会被注册到Selector
    private final ConcurrentLinkedQueue<IoTask> mRegisterTaskQueue = new ConcurrentLinkedQueue<>();
    // 任务饱和度
    private final AtomicLong mSaturatingCapacity = new AtomicLong();
    private volatile StealingService mStealingService;
    private final AtomicBoolean unRegisterLocker = new AtomicBoolean(false);

    public StealingSelectorThread(Selector selector) {
        super("StealingSelector-Thread");
        this.selector = selector;
    }

    public void setStealingService(StealingService service) {
        this.mStealingService = service;
    }

    /**
     * 调用子类执行任务操作
     *
     * @param task 任务
     * @return 执行任务后是否需要再次添加该任务
     */
    protected abstract boolean processTask(IoTask task);

    /**
     * 将通道注册到当前的Selector中
     */
    public void register(IoTask ioTask) {
        if ((ioTask.ops & ~VALID_OPS) != 0) {
            throw new UnsupportedOperationException("Unsupported register ops:" + ioTask.ops);
        }
        mRegisterTaskQueue.offer(ioTask);
        selector.wakeup(); // 唤醒阻塞
    }

    /**
     * 取消注册，原理类似于注册操作在队列中添加一份取消注册的任务；并将副本变量清空
     *
     * @param channel 通道
     */
    public void unregister(SocketChannel channel) {
        SelectionKey selectionKey = channel.keyFor(selector);
        if (selectionKey != null && selectionKey.attachment() != null) {
            // 关闭前可使用Attach简单判断是否已处于队列中
            selectionKey.attach(null);
            if (Thread.currentThread() == this) {
                selectionKey.cancel();
            } else {
                // 保障同步与原子操作
                synchronized (unRegisterLocker) {
                    unRegisterLocker.set(true);
                    selector.wakeup(); // 唤醒会开始循环
                    selectionKey.cancel();
                    unRegisterLocker.set(false);
                }
            }
        }
    }

    /**
     * 获取内部的任务队列
     */
    Queue<IoTask> getReadyTaskQueue() {
        return mReadyTaskQueue;
    }

    /**
     * 获取饱和程度
     * 暂时的饱和度量是使用任务执行的次数来定
     *
     * @return -1 已失效
     */
    long getSaturatingCapacity() {
        if (selector.isOpen()) {
            return mSaturatingCapacity.get();
        }
        return -1;
    }


    public void exit() {
        isRunning = false;
        CloseUtils.close(selector);
        interrupt();
    }

    /**
     * 消费当前待注册的通道任务
     *
     * @param registerTaskQueue 待注册的通道
     */
    private void consumeRegisterTodoTasks(final ConcurrentLinkedQueue<IoTask> registerTaskQueue) {
        final Selector selector = this.selector;
        IoTask registerTask = registerTaskQueue.poll();
        while (registerTask != null) {
            try {
                final SocketChannel channel = registerTask.channel;
                int ops = registerTask.ops;

                SelectionKey key = channel.keyFor(selector);
                if (key == null) {
                    key = channel.register(selector, ops, new KeyAttachment());
                } else {
                    key.interestOps(key.interestOps() | ops);
                }

                Object attachment = key.attachment();
                if (attachment instanceof KeyAttachment) {
                    ((KeyAttachment) attachment).attach(ops, registerTask);
                } else {
                    key.cancel(); // 直接取消
                }

            } catch (ClosedChannelException | CancelledKeyException | ClosedSelectorException e) {
                e.printStackTrace();
                registerTask.fireThrowable(e);
            } finally {
                registerTask = registerTaskQueue.poll();
            }
        }
    }

    /**
     * 单次就绪的任务加入缓存队列
     * onceReadyTaskCache
     *
     * @param readyTaskQueue     已就绪总任务队列
     * @param onceReadyTaskCache 单次待执行任务
     */
    private void joinTaskQueue(final Queue<IoTask> readyTaskQueue, final List<IoTask> onceReadyTaskCache) {
        readyTaskQueue.addAll(onceReadyTaskCache);
    }

    /**
     * 消费待完成的任务
     */
    private void consumeTodoTasks(final Queue<IoTask> readyTaskQueue, ConcurrentLinkedQueue<IoTask> registerTaskQueue) {
        final AtomicLong saturatingCapacity = this.mSaturatingCapacity;
        IoTask task = readyTaskQueue.poll();
        while (task != null) {
            // 增加饱和度
            saturatingCapacity.incrementAndGet();

            if (processTask(task)) { // 返回true，继续关注该任务，再次加入
                registerTaskQueue.offer(task);
            }
            // 取下一个任务
            task = readyTaskQueue.poll();
        }

        // 窃取其他任务
        final StealingService stealingService = this.mStealingService;
        if (stealingService != null) {
            task = stealingService.steal(readyTaskQueue);
            while (task != null) {
                saturatingCapacity.incrementAndGet();
                if (processTask(task)) {
                    registerTaskQueue.offer(task);
                }
                task = stealingService.steal(readyTaskQueue);
            }
        }
    }


    @Override
    public void run() {
        super.run();

        final Selector selector = this.selector;
        final ArrayBlockingQueue<IoTask> readyTaskQueue = this.mReadyTaskQueue;
        final ConcurrentLinkedQueue<IoTask> registerTaskQueue = this.mRegisterTaskQueue;
        final AtomicBoolean unRegisterLocker = this.unRegisterLocker;
        final List<IoTask> onceReadyReadCache = new ArrayList<>(ONCE_READ_TASK_MAX);
        final List<IoTask> onceReadyWriteCache = new ArrayList<>(ONCE_WRITE_TASK_MAX);

        try {
            while (isRunning) {
                // 加入待注册的通道
                consumeRegisterTodoTasks(registerTaskQueue);

                int count = selector.select();
                while (unRegisterLocker.get()) {
                    Thread.yield();
                }
                if (count == 0) {
                    continue;
                }

                // 处理已就绪的通道
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();

                int onceReadTaskCount = ONCE_READ_TASK_MAX;
                int onceWriteTaskCount = ONCE_WRITE_TASK_MAX;

                // 处理已就绪的任务
                while (iterator.hasNext()) {
                    SelectionKey selectionKey = iterator.next();
                    Object attachmentObj = selectionKey.attachment();
                    if (selectionKey.isValid() && attachmentObj instanceof KeyAttachment) {
                        final KeyAttachment attachment = (KeyAttachment) attachmentObj;
                        try {
                            final int readyOps = selectionKey.readyOps();
                            int interestOps = selectionKey.interestOps(); // 关注的事件类型

                            // 是否可读
                            if ((readyOps & SelectionKey.OP_READ) != 0 && onceReadTaskCount-- > 0) {
                                onceReadyReadCache.add(attachment.taskForReadable);
                                interestOps = interestOps & ~SelectionKey.OP_READ;
                            }

                            // 是否可写
                            if ((readyOps & SelectionKey.OP_WRITE) != 0 && onceWriteTaskCount-- > 0) {
                                onceReadyWriteCache.add(attachment.taskForWritable);
                                interestOps = interestOps & ~SelectionKey.OP_WRITE;
                            }
                            // 取消已就绪的关注
                            selectionKey.interestOps(interestOps);
                        } catch (CancelledKeyException e) {
                            if (attachment.taskForReadable != null) {
                                onceReadyReadCache.remove(attachment.taskForReadable);
                            }
                            if (attachment.taskForWritable != null) {
                                onceReadyWriteCache.remove(attachment.taskForWritable);
                            }
                        }
                    }
                    iterator.remove();
                }

                // 判断本次是否有待执行的任务
                if (!onceReadyReadCache.isEmpty()) {
                    // 加入到总队列
                    joinTaskQueue(readyTaskQueue, onceReadyReadCache);
                    onceReadyReadCache.clear();
                }
                if (!onceReadyWriteCache.isEmpty()) {
                    // 加入到总队列
                    joinTaskQueue(readyTaskQueue, onceReadyWriteCache);
                    onceReadyWriteCache.clear();
                }

                // 消费总队列中的任务
                consumeTodoTasks(readyTaskQueue, registerTaskQueue);
            }
        } catch (ClosedSelectorException e) {
            e.printStackTrace();
        } catch (IOException e) {
            CloseUtils.close(selector);
        } finally {
            readyTaskQueue.clear();
            registerTaskQueue.clear();
        }
    }

    /**
     * 注册时添加的附件
     */
    static class KeyAttachment {
        // 可读时执行的任务
        IoTask taskForReadable;
        // 可写时执行的任务
        IoTask taskForWritable;

        /**
         * 附加任务
         *
         * @param ops  任务关注的事件类型
         * @param task 任务
         */
        void attach(int ops, IoTask task) {
            if (ops == SelectionKey.OP_READ) {
                taskForReadable = task;
            } else {
                taskForWritable = task;
            }
        }
    }
}
