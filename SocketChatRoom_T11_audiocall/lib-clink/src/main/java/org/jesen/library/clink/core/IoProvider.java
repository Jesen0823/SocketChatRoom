package org.jesen.library.clink.core;

import java.io.Closeable;
import java.nio.channels.SocketChannel;

public interface IoProvider extends Closeable {
    // 注册一个回调，从SocketChannel中读取数据，观察SocketChannel的可读状态
    boolean registerInput(SocketChannel channel, HandleProviderCallback callback);

    // 注册一个回调，想要发送数据
    boolean registerOutput(SocketChannel channel, HandleProviderCallback callback);

    void unRegisterInput(SocketChannel channel);

    void unRegisterOutput(SocketChannel channel);

    abstract class HandleProviderCallback implements Runnable {
        // 上一份没有发送完成的IoArgs
        protected volatile IoArgs attach;

        @Override
        public final void run() {
            onProviderTo(attach);
        }

        protected abstract void onProviderTo(IoArgs args);

        public void checkAttachNull() {
            if (attach != null) {
                throw new IllegalStateException("Current attach is not null.");
            }
        }
    }

}

