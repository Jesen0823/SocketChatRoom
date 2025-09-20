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
        // 附加参数，用来存储SocketChannel可发送状态时，要发送的数据
        private volatile IoArgs attach;

        @Override
        public final void run() {
            onProviderIo(attach);
        }

        public final void setAttach(IoArgs attach) {
            this.attach = attach;
        }

        protected abstract void onProviderIo(IoArgs attach);

        public final <T> T getAttach() {
            @SuppressWarnings({"UnnecessaryLocalVariable", "unchecked"})
            T attach = (T) this.attach;
            return attach;
        }

        public void checkAttachNull(){
            if (attach!=null){
                throw new IllegalStateException("Current attach is not empty.");
            }
        }
    }

}

