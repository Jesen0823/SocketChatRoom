package net.qiujuer.library.clink.core;

import java.io.Closeable;
import java.nio.channels.SocketChannel;

public interface IoProvider extends Closeable {
    // 注册一个回调，从SocketChannel中读取数据，观察SocketChannel的可读状态
    boolean registerInput(SocketChannel channel, HandleInputCallback callback);

    // 注册一个回调，想要发送数据
    boolean registerOutput(SocketChannel channel, HandleOutputCallback callback);

    void unRegisterInput(SocketChannel channel);

    void unRegisterOutput(SocketChannel channel);

    abstract class HandleInputCallback implements Runnable {
        @Override
        public final void run() {
            canProviderInput();
        }

        protected abstract void canProviderInput();
    }

    abstract class HandleOutputCallback implements Runnable {
        // 附加参数，用来存储SocketChannel可发送状态时，要发送的数据
        private Object attach;

        @Override
        public final void run() {
            canProviderOutput(attach);
        }

        public final void setAttach(Object attach) {
            this.attach = attach;
        }

        protected abstract void canProviderOutput(Object attach);

        public final <T> T getAttach() {
            @SuppressWarnings({"UnnecessaryLocalVariable", "unchecked"})
            T attach = (T) this.attach;
            return attach;
        }
    }

}

