package net.qiujuer.library.clink.impl.bridge;

import net.qiujuer.library.clink.core.*;
import net.qiujuer.library.clink.impl.exceptions.EmptyIoArgsException;
import net.qiujuer.library.clink.utils.plugin.CircularByteBuffer;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 桥接调度器实现
 * 当前调度器同时实现了发送者与接受者调度逻辑
 * 核心思想为：把接受者接收到的数据全部转发给发送者
 */
public class BridgeSocketDispatcher implements ReceiveDispatcher, SendDispatcher {
    // 环形缓冲区用来暂存数据
    private final CircularByteBuffer buffer = new CircularByteBuffer(512, true);
    // 读取写入通道
    private final ReadableByteChannel readableByteChannel = Channels.newChannel(buffer.getInputStream());
    private final WritableByteChannel writableByteChannel = Channels.newChannel(buffer.getOutputStream());
    // 有数据则接收，无数据不强求填满，有多少返回多少
    private final IoArgs receiveIoArgs = new IoArgs(256, false);
    // 用来发送的IoArgs，默认全部发送
    private final IoArgs sendIoArgs = new IoArgs();
    // 当前是否处于发送中
    private final AtomicBoolean isSending = new AtomicBoolean();
    private final Receiver receiver;
    private Sender sender;

    private final IoArgs.IoArgsEventListener senderEventProcessor = new IoArgs.IoArgsEventListener() {

        @Override
        public IoArgs provideIoArgs() {
            try {
                int available = buffer.getAvailable();
                IoArgs args = BridgeSocketDispatcher.this.sendIoArgs;
                if (available > 0) {
                    args.limit(available);
                    args.startWriting();
                    args.readFrom(readableByteChannel);
                    args.finishWriting();
                    return args;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public boolean onConsumeFailed(IoArgs args, Throwable e) {
            synchronized (isSending){
                isSending.set(false);
            }
            // 继续请求发送当前数据
            requestSend();
            return false;
        }

        @Override
        public boolean onConsumeCompleted(IoArgs args) {
            synchronized (isSending){
                isSending.set(false);
            }
            // 继续请求发送当前数据
            requestSend();
            return false;
        }
    };
    private final IoArgs.IoArgsEventListener receiveEventProcessor = new IoArgs.IoArgsEventListener() {

        @Override
        public IoArgs provideIoArgs() {
            receiveIoArgs.resetLimit();
            receiveIoArgs.startWriting();
            return receiveIoArgs;
        }

        @Override
        public boolean onConsumeFailed(IoArgs args, Throwable e) {
            return false;
        }

        @Override
        public boolean onConsumeCompleted(IoArgs args) {
            args.finishWriting();
            try {
                args.writeTo(writableByteChannel);
            } catch (IOException e) {
                e.printStackTrace();
            }
            registerReceive();
            requestSend();
            return false;
        }
    };

    private void registerReceive() {

    }

    private void requestSend() {
        synchronized (isSending) {
            final Sender sender = this.sender;
            if (isSending.get() || sender == null) {
                return;
            }
            // 返回true代表当前有数据需要发送
            if (buffer.getAvailable() > 0) {
                try {
                    boolean isSucceed = sender.postSendAsync();
                    if (isSucceed) {
                        isSending.set(true);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public BridgeSocketDispatcher(Receiver receiver) {
        this.receiver = receiver;
    }

    /**
     * 绑定一个新的发送者，将老的发送者对应的调度设置为null
     */
    public void bindSender(Sender sender) {
        final Sender oldSender = this.sender;
        if (oldSender != null) {
            oldSender.setSendListener(null);
        }
        synchronized (isSending) {
            isSending.set(false);
        }
        buffer.clear();
        // 设置新的发送者
        this.sender = sender;
        if (sender != null) {
            sender.setSendListener(senderEventProcessor);
            requestSend();
        }
    }

    /**
     * 外部初始化好了桥接调度器后需要调用start方法开始
     */
    @Override
    public void start() {
        receiver.setReceiveListener(receiveEventProcessor);
        requestReceive();
    }

    @Override
    public void stop() {
        // nothing
        receiver.setReceiveListener(null);
        sender.setSendListener(null);
    }

    @Override
    public void send(SendPacket packet) {
        // nothing
    }

    @Override
    public void sendHeartbeat() {
        // nothing
    }

    @Override
    public void cancel(SendPacket packet) {
        // nothing
    }

    @Override
    public void close() {
        // nothing
    }

    private synchronized void requestReceive() {
        try {
            receiver.postReceiveAsync();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getSpaceReading() {
        return buffer.getAvailable();
    }

    private int getSpaceWriting() {
        return buffer.getSpaceLeft();
    }
}
