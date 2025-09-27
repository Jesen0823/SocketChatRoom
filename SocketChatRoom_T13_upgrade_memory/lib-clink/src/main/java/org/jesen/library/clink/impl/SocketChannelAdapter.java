package org.jesen.library.clink.impl;

import org.jesen.library.clink.core.IoArgs;
import org.jesen.library.clink.core.IoProvider;
import org.jesen.library.clink.core.Receiver;
import org.jesen.library.clink.core.Sender;
import org.jesen.library.clink.impl.exceptions.EmptyIoArgsException;
import org.jesen.library.clink.utils.CloseUtils;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 发送与接收的实现
 */

public class SocketChannelAdapter implements Sender, Receiver, Cloneable {
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    // 发送或接收通道
    private final SocketChannel channel;
    private final IoProvider ioProvider;
    private final OnChannelStatusChangedListener listener;
    private final AbsProviderCallback inputCallback;
    private final AbsProviderCallback outputCallback;

    public SocketChannelAdapter(SocketChannel channel, IoProvider ioProvider,
                                OnChannelStatusChangedListener listener) throws IOException {
        this.channel = channel;
        this.ioProvider = ioProvider;
        this.listener = listener;
        this.inputCallback = new InputProviderCallback(channel, SelectionKey.OP_READ, ioProvider);
        this.outputCallback = new OutputProviderCallback(channel, SelectionKey.OP_WRITE, ioProvider);
    }

    @Override
    public long getLastReadTime() {
        return inputCallback.lastActiveTime;
    }

    @Override
    public long getLastWriteTime() {
        return outputCallback.lastActiveTime;
    }

    @Override
    public void setReceiveListener(IoArgs.IoArgsEventProcessor processor) {
        if (inputCallback.eventProcessor!=processor){
            ioProvider.unRegister(channel);
        }
        inputCallback.eventProcessor = processor;
    }

    @Override
    public void setSendListener(IoArgs.IoArgsEventProcessor processor) {
        outputCallback.eventProcessor = processor;
    }

    @Override
    public void postReceiveAsync() throws Exception {
        if (isClosed.get() || !channel.isOpen()) {
            throw new IOException("Current channel is closed!");
        }
        // 检测Callback状态是否处于自循环状态
        inputCallback.checkAttachNull();
        ioProvider.register(inputCallback);
    }

    @Override
    public void postSendAsync() throws Exception {
        if (isClosed.get() || !channel.isOpen()) {
            throw new IOException("Current channel is closed!");
        }
        // 检测Callback状态是否处于自循环状态
        outputCallback.checkAttachNull();
        ioProvider.register(outputCallback);
    }


    @Override
    public void close() {
        if (isClosed.compareAndSet(false, true)) {
            // 解除注册回调
            ioProvider.unRegister(channel);
            // 关闭
            CloseUtils.close(channel);
            // 回调当前Channel已关闭
            listener.onChannelClosed(channel);
        }
    }

    class InputProviderCallback extends AbsProviderCallback {

        InputProviderCallback(SocketChannel channel, int ops, IoProvider ioProvider) {
            super(channel, ops, ioProvider);
        }

        @Override
        protected int consumeIoArgs(IoArgs args, SocketChannel channel) throws IOException {
            return args.readFrom(channel);
        }
    }

    class OutputProviderCallback extends AbsProviderCallback {

        OutputProviderCallback(SocketChannel channel, int ops, IoProvider ioProvider) {
            super(channel, ops, ioProvider);
        }

        @Override
        protected int consumeIoArgs(IoArgs args, SocketChannel channel) throws IOException {
            return args.writeTo(channel);
        }
    }

    abstract class AbsProviderCallback extends IoProvider.HandleProviderCallback {
        volatile IoArgs.IoArgsEventProcessor eventProcessor;
        volatile long lastActiveTime = System.currentTimeMillis();

        AbsProviderCallback(SocketChannel channel, int ops, IoProvider ioProvider) {
            super(channel, ops, ioProvider);
        }

        @Override
        protected boolean onProviderIo(IoArgs args) {
            if (isClosed.get()) {
                return false;
            }

            IoArgs.IoArgsEventProcessor processor = eventProcessor;
            if (processor == null) {
                return false;
            }
            lastActiveTime = System.currentTimeMillis();
            if (args == null) {
                // 没有上次剩余的，吃的很干净，获取新的IoArgs
                args = processor.provideIoArgs();
            }
            try {
                if (args == null) {
                    throw new EmptyIoArgsException("ProvideIoArgs is null.");
                }
                // 具体的写入操作
                int count = consumeIoArgs(args, channel);
                // 是否还有未消费数据，且是否需要一次性消费完
                if (args.remained() && (count == 0 || args.isNeedConsumeRemaining())) {
                    // 当前Args还可以发送数据，再次注册,会加入队列，绑定Select
                    this.attach = args; // 保留未发送完的IoArgs,附加到callback对象
                    return true;
                } else {
                    return processor.onConsumeCompleted(args); // 写入完成回调
                }
            } catch (IOException e) {
                if (processor.onConsumeFailed(e)) {
                    CloseUtils.close(SocketChannelAdapter.this);
                }
                return false;
            }
        }
        @Override
        public void fireThrowable(Throwable e) {
            final IoArgs.IoArgsEventProcessor processor = this.eventProcessor;
            if (processor == null || processor.onConsumeFailed(e)) {
                CloseUtils.close(SocketChannelAdapter.this);
            }
        }

        protected abstract int consumeIoArgs(IoArgs args, SocketChannel channel) throws IOException;
    }

    public interface OnChannelStatusChangedListener {
        void onChannelClosed(SocketChannel channel);
    }
}
