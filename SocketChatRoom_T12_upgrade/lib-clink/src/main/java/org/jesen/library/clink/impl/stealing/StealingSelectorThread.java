package org.jesen.library.clink.impl.stealing;

import org.jesen.library.clink.core.IoProvider;
import org.jesen.library.clink.utils.CloseUtils;

import java.io.IOException;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 可窃取任务的线程
 */
public abstract class StealingSelectorThread extends Thread {
    // 允许的操作
    private static final int VALID_OPS = SelectionKey.OP_READ | SelectionKey.OP_WRITE;
    private final Selector selector;
    private volatile boolean isRunning = true;
    // 当前Selector中已就绪的任务队列
    private final LinkedBlockingQueue<IoTask> mReadyTaskQueue = new LinkedBlockingQueue<>();
    // 暂存待注册任务队列，条件允许时将会被注册到Selector
    private final LinkedBlockingQueue<IoTask> mRegisterTaskQueue = new LinkedBlockingQueue<>();
    // Selector.select()后，单次已就绪的任务缓存，暂存，后续会一次性加入到就绪队列 mReadyTaskQueue
    private final List<IoTask> mOnceReadyTaskCache = new ArrayList<>(200);

    public StealingSelectorThread(Selector selector) {
        super("StealingSelector-Thread");
        this.selector = selector;
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
     *
     * @param channel  通道
     * @param ops      关注的行为
     * @param callback 触发时的回调
     * @return 是否注册成功
     */
    public boolean register(SocketChannel channel, int ops, IoProvider.HandleProviderCallback callback) {
        if (channel.isOpen()) {
            IoTask ioTask = new IoTask(channel, callback, ops);
            mRegisterTaskQueue.offer(ioTask);
            return true;
        }
        return false;
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
            // 添加取消操作
            IoTask ioTask = new IoTask(channel, null, 0);
            mRegisterTaskQueue.offer(ioTask);
        }
    }

    /**
     * 获取内部的任务队列
     */
    Queue<IoTask> getReadyTaskQueue() {
        return mReadyTaskQueue;
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
    private void consumeRegisterTodoTasks(final LinkedBlockingQueue<IoTask> registerTaskQueue) {
        final Selector selector = this.selector;
        IoTask registerTask = registerTaskQueue.poll();
        while (registerTask != null) {
            try {
                final SocketChannel channel = registerTask.channel;
                int ops = registerTask.ops;
                if (ops == 0) {
                    SelectionKey key = channel.keyFor(selector);
                    if (key != null) {
                        key.cancel();
                    }
                } else if ((ops & ~VALID_OPS) == 0) {
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
                }
            } catch (ClosedChannelException | CancelledKeyException | ClosedSelectorException e) {
                e.printStackTrace();
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
    private void joinTaskQueue(final LinkedBlockingQueue<IoTask> readyTaskQueue, final List<IoTask> onceReadyTaskCache) {
        readyTaskQueue.addAll(onceReadyTaskCache);
    }

    /**
     * 消费待完成的任务
     */
    private void consumeTodoTasks(final LinkedBlockingQueue<IoTask> readyTaskQueue, LinkedBlockingQueue<IoTask> registerTaskQueue) {
        IoTask task = readyTaskQueue.poll();
        while (task != null) {
            if (processTask(task)) { // 返回true，继续关注该任务，再次加入
                registerTaskQueue.offer(task);
            }
            // 取下一个任务
            task = readyTaskQueue.poll();
        }
    }


    @Override
    public void run() {
        super.run();

        final Selector selector = this.selector;
        final LinkedBlockingQueue<IoTask> readyTaskQueue = this.mReadyTaskQueue;
        final LinkedBlockingQueue<IoTask> registerTaskQueue = this.mRegisterTaskQueue;
        final List<IoTask> onceReadyTaskCache = this.mOnceReadyTaskCache;

        try {
            while (isRunning) {
                // 加入待注册的通道
                consumeRegisterTodoTasks(registerTaskQueue);

                // 检查一次是否有就绪
                if ((selector.selectNow()) == 0) {
                    Thread.yield();
                    continue;
                }

                // 处理已就绪的通道
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();

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
                            if ((readyOps & SelectionKey.OP_READ) != 0) {
                                onceReadyTaskCache.add(attachment.taskForReadable);
                                interestOps = interestOps & ~SelectionKey.OP_READ;
                            }

                            // 是否可写
                            if ((readyOps & SelectionKey.OP_WRITE) != 0) {
                                onceReadyTaskCache.add(attachment.taskForWritable);
                                interestOps = interestOps & ~SelectionKey.OP_WRITE;
                            }
                            // 取消已就绪的关注
                            selectionKey.interestOps(interestOps);
                        } catch (CancelledKeyException e) {
                            onceReadyTaskCache.remove(attachment.taskForReadable);
                            onceReadyTaskCache.remove(attachment.taskForWritable);
                        }
                    }
                    iterator.remove();
                }

                // 判断本次是否有待执行的任务
                if (!onceReadyTaskCache.isEmpty()) {
                    // 加入到总队列
                    joinTaskQueue(readyTaskQueue, onceReadyTaskCache);
                    onceReadyTaskCache.clear();
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
            onceReadyTaskCache.clear();
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
