package org.jesen.library.clink.impl;

import org.jesen.library.clink.core.IoProvider;
import org.jesen.library.clink.core.IoTask;
import org.jesen.library.clink.impl.stealing.StealingSelectorThread;
import org.jesen.library.clink.impl.stealing.StealingService;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * 可窃取任务的IoProvider
 */
public class IoStealingSelectorProvider implements IoProvider {
    private final IoStealingThread[] mThreads;
    private final StealingService mStealingService;

    public IoStealingSelectorProvider(int pooSize) throws IOException {
        IoStealingThread[] threads = new IoStealingThread[pooSize];
        for (int i = 0; i < pooSize; i++) {
            Selector selector = Selector.open();
            threads[i] = new IoStealingThread("IoProvider-IoStealingThread" + (i + 1), selector);
        }

        StealingService stealingService = new StealingService(10, threads);
        for (IoStealingThread thread : threads) {
            thread.setStealingService(stealingService);
            thread.setDaemon(false);
            thread.setPriority(Thread.MAX_PRIORITY);
            thread.start();
        }

        this.mStealingService = stealingService;
        this.mThreads = threads;
    }

    @Override
    public void close() throws IOException {
        mStealingService.shutdown();
    }

    @Override
    public void register(HandleProviderCallback callback) throws Exception {
        StealingSelectorThread thread = mStealingService.getNotBusyThread();
        if (thread == null) {
            throw new IOException("IoStealingSelectorProvider is shutdown.");
        }
        thread.register(callback);
    }

    @Override
    public void unRegister(SocketChannel channel) {
        if (!channel.isOpen()) {
            return;
        }
        for (IoStealingThread thread : mThreads) {
            thread.unregister(channel);
        }
    }

    static class IoStealingThread extends StealingSelectorThread {

        public IoStealingThread(String name, Selector selector) {
            super(selector);
            setName(name);
        }

        @Override
        protected boolean processTask(IoTask task) {
            return task.onProcess();
        }
    }
}
