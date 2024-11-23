package net.qiujuer.library.clink.impl;

import net.qiujuer.library.clink.core.IoArgs;
import net.qiujuer.library.clink.core.IoProvider;
import net.qiujuer.library.clink.core.Receiver;
import net.qiujuer.library.clink.core.Sender;
import net.qiujuer.library.clink.utils.CloseUtils;

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

    private IoArgs.IoArgsEventListener receiveIoEventListener;
    private IoArgs.IoArgsEventListener sendIoEventListener;

    private final IoProvider.HandleProviderCallback inputCallback = new IoProvider.HandleProviderCallback() {
        @Override
        protected void onProviderIo(IoArgs args) {
            if (isClosed.get()) {
                return;
            }
            IoArgs.IoArgsEventListener listener = receiveIoEventListener;
            // 拿到新的Args
            if (args == null) {
                args = listener.provideIoArgs();
            }
            try {
                if (args == null) {
                    listener.onConsumeFailed(null, new IOException("Provide IoArgs is null"));
                } else {
                    int count = args.readFrom(channel);
                    if (count == 0) {
                        System.out.println("Current read zero data");
                    }
                    if (args.remained()) {
                        // 附加当前未消费完成的args
                        setAttach(args);
                        // 再次注册数据发送
                        ioProvider.registerInput(channel, this);
                    } else {
                        setAttach(null);
                        // 读取数据完成
                        listener.onConsumeCompleted(args);
                    }
                }
            } catch (IOException ignored) {
                CloseUtils.close(SocketChannelAdapter.this);
            }
        }
    };

    private final IoProvider.HandleProviderCallback outputCallback = new IoProvider.HandleProviderCallback() {

        @Override
        protected void onProviderIo(IoArgs args) {
            if (isClosed.get()) {
                return;
            }
            IoArgs.IoArgsEventListener listener = sendIoEventListener;
            // 拿到新的Args
            if (args == null) {
                args = listener.provideIoArgs();
            }

            try {
                if (args == null) {
                    listener.onConsumeFailed(null, new IOException("Provider args is null"));
                } else {
                    int count = args.writeTo(channel);
                    if (count == 0) {
                        System.out.println("Current write zero data");
                    }
                    if (args.remained()) {
                        // 附加当前未消费完成的args
                        setAttach(args);
                        // 再次注册数据发送
                        ioProvider.registerOutput(channel, this);
                    } else {
                        setAttach(null);
                        listener.onConsumeCompleted(args);
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
    public long getLastReadTime() {
        return 0;
    }

    @Override
    public void setReceiveListener(IoArgs.IoArgsEventListener listener) {
        receiveIoEventListener = listener;
    }

    @Override
    public void setSendListener(IoArgs.IoArgsEventListener listener) {
        sendIoEventListener = listener;
    }

    @Override
    public boolean postReceiveAsync() throws Exception {
        if (isClosed.get()) {
            throw new IOException("Current channel is closed!");
        }
        // 进行Callback状态监测，判断是否处于自循环状态
        inputCallback.checkAttachNull();
        return ioProvider.registerInput(channel,inputCallback);
    }

    @Override
    public boolean postSendAsync() throws IOException {
        if (isClosed.get()){
            throw new IOException("Current channel is closed.");
        }
        // 进行Callback状态监测，判断是否处于自循环状态
        inputCallback.checkAttachNull();
        return ioProvider.registerOutput(channel, outputCallback);
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

    public interface OnChannelStatusChangedListener {
        void onChannelClosed(SocketChannel channel);
    }
}
