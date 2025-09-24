package org.jesen.library.clink.impl;

import org.jesen.library.clink.core.IoArgs;
import org.jesen.library.clink.core.IoProvider;
import org.jesen.library.clink.core.Receiver;
import org.jesen.library.clink.core.Sender;
import org.jesen.library.clink.utils.CloseUtils;

import java.io.IOException;
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

    private IoArgs.IoArgsEventProcessor receiveIoEventProcessor;
    private IoArgs.IoArgsEventProcessor sendIoEventProcessor;

    private volatile long lastReadTime = System.currentTimeMillis();
    private volatile long lastWriteTime = System.currentTimeMillis();

    private final IoProvider.HandleProviderCallback inputCallback = new IoProvider.HandleProviderCallback() {
        @Override
        protected void onProviderTo(IoArgs args) {
            if (isClosed.get()) {
                return;
            }

            lastReadTime = System.currentTimeMillis();

            IoArgs.IoArgsEventProcessor processor = receiveIoEventProcessor;
            if (args == null) {
                args = processor.provideIoArgs();
            }

            try {
                // 具体的读取操作
                if (args == null) {
                    processor.onConsumeFailed(null, new IOException("provider IoArgs is null."));
                } else {
                    int count = args.readFrom(channel);
                    if (count == 0) {
                        // 当前的Channel无法发送数据
                        System.out.println("Current read zero data.");
                    }
                    if (args.remained()) {
                        // 当前Args还可以发送数据，再次注册,会加入队列，绑定Select
                        this.attach = args; // 保留未发送完的IoArgs,附加到callback对象
                        ioProvider.registerInput(channel, this);
                    } else {
                        this.attach = null;
                        // 写入完成回调
                        processor.onConsumeCompleted(args);
                    }
                }
            } catch (IOException ignored) {
                CloseUtils.close(SocketChannelAdapter.this);
            }
        }
    };

    private final IoProvider.HandleProviderCallback outputCallback = new IoProvider.HandleProviderCallback() {
        @Override
        protected void onProviderTo(IoArgs args) {  // args上一份没有发送完成的IoArgs
            if (isClosed.get()) {
                return;
            }

            lastWriteTime = System.currentTimeMillis();

            IoArgs.IoArgsEventProcessor processor = sendIoEventProcessor;
            if (args == null) {
                // 没有上次剩余的，吃的很干净，获取新的IoArgs
                args = processor.provideIoArgs();
            }
            try {
                if (args == null) {
                    processor.onConsumeFailed(null, new IOException("ProvideIoArgs is null."));
                } else {
                    // 具体的写入操作
                    int count = args.writeTo(channel);
                    if (count == 0) {
                        // 当前的Channel无法发送数据
                        System.out.println("Current write zero data.");
                    }
                    if (args.remained()) {
                        // 当前Args还可以发送数据，再次注册,会加入队列，绑定Select
                        this.attach = args; // 保留未发送完的IoArgs,附加到callback对象
                        ioProvider.registerOutput(channel, this);
                    } else {
                        this.attach = null;
                        // 写入完成回调
                        processor.onConsumeCompleted(args);
                    }
                }
            } catch (IOException ignored) {
                CloseUtils.close(SocketChannelAdapter.this);
            }
        }
    };

    public SocketChannelAdapter(SocketChannel channel, IoProvider ioProvider,
                                OnChannelStatusChangedListener listener) throws IOException {
        this.channel = channel;
        this.ioProvider = ioProvider;
        this.listener = listener;

        channel.configureBlocking(false);
    }

    @Override
    public void close() throws IOException {
        if (isClosed.compareAndSet(false, true)) {
            // 解除注册回调
            ioProvider.unRegisterInput(channel);
            ioProvider.unRegisterOutput(channel);
            // 关闭
            CloseUtils.close(channel);
            // 回调当前Channel已关闭
            listener.onChannelClosed(channel);
        }
    }

    @Override
    public void setReceiveListener(IoArgs.IoArgsEventProcessor processor) {
        receiveIoEventProcessor = processor;
    }

    @Override
    public boolean postReceiveAsync() throws IOException {
        if (isClosed.get()) {
            throw new IOException("Current channel is closed!");
        }
        // 检测Callback状态是否处于自循环状态
        inputCallback.checkAttachNull();
        return ioProvider.registerInput(channel, inputCallback);
    }

    @Override
    public long getLastReadTime() {
        return lastReadTime;
    }

    @Override
    public void setSendListener(IoArgs.IoArgsEventProcessor processor) {
        sendIoEventProcessor = processor;
    }

    @Override
    public boolean postSendAsync() throws IOException {
        if (isClosed.get()) {
            throw new IOException("Current channel is closed!");
        }
        // 检测Callback状态是否处于自循环状态
        inputCallback.checkAttachNull();
        return ioProvider.registerOutput(channel, outputCallback);
    }

    @Override
    public long getLastWriteTime() {
        return lastWriteTime;
    }

    public interface OnChannelStatusChangedListener {
        void onChannelClosed(SocketChannel channel);
    }
}
