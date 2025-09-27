package org.jesen.library.clink.core;

import java.io.Closeable;
import java.nio.channels.SocketChannel;

public interface IoProvider extends Closeable {
    // 注册一个回调，从SocketChannel中读取数据，观察SocketChannel的可读可写状态
    void register(HandleProviderCallback callback) throws Exception;

    void unRegister(SocketChannel channel);

    abstract class HandleProviderCallback extends IoTask implements Runnable {
        private final IoProvider ioProvider;
        // 上一份没有发送完成的IoArgs
        protected volatile IoArgs attach;

        public HandleProviderCallback(SocketChannel channel, int ops, IoProvider ioProvider) {
            super(channel, ops);
            this.ioProvider = ioProvider;
        }

        @Override
        public final void run() {
            final IoArgs attach = this.attach;
            this.attach = null;
            if (onProviderIo(attach)) {
                try {
                    ioProvider.register(this);
                } catch (Exception e) {
                    fireThrowable(e); // 调用到父类IoTask
                }
            }
        }

        @Override
        public final boolean onProcess() {
            final IoArgs attach = this.attach;
            this.attach = null;
            return onProviderIo(attach);
        }

        /**
         * 可接受/可发送时的回调
         *
         * @return 返回当前状态，是否还需要后续调度,返回true表示数据需要再次注册
         */
        protected abstract boolean onProviderIo(IoArgs args);

        public void checkAttachNull() {
            if (attach != null) {
                throw new IllegalStateException("Current attach is not null.");
            }
        }
    }
}

