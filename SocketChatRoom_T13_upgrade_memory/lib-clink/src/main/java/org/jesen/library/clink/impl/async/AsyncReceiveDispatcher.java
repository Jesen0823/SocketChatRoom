package org.jesen.library.clink.impl.async;

import org.jesen.library.clink.core.IoArgs;
import org.jesen.library.clink.core.ReceiveDispatcher;
import org.jesen.library.clink.core.ReceivePacket;
import org.jesen.library.clink.core.Receiver;
import org.jesen.library.clink.utils.CloseUtils;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 写数据到Packet
 * 注册Receiver
 */
public class AsyncReceiveDispatcher implements ReceiveDispatcher, IoArgs.IoArgsEventProcessor,
        AsyncPacketWriter.PacketProvider {

    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final Receiver receiver;
    private final ReceivePacketCallback receiveCallback;
    private final AsyncPacketWriter writer = new AsyncPacketWriter(this);

    public AsyncReceiveDispatcher(Receiver receiver, ReceivePacketCallback receiveCallback) {
        this.receiver = receiver;
        this.receiveCallback = receiveCallback;
        this.receiver.setReceiveListener(this);
    }

    @Override
    public void start() {
        registerReceive();
    }


    @Override
    public void stop() {
        System.out.println("--AsyncReceiveDispatcher,stop.");
        receiver.setReceiveListener(null);
    }

    @Override
    public void close() throws IOException {
        if (isClosed.compareAndSet(false, true)) {
            writer.close();
            receiver.setReceiveListener(null);
        }
    }

    private void registerReceive() {
        try {
            receiver.postReceiveAsync();
        } catch (Exception e) {
            e.printStackTrace();
            CloseUtils.close(this);
        }
    }

    @Override
    public IoArgs provideIoArgs() {
        IoArgs ioArgs = writer.takeIoArgs();
        ioArgs.startWriting();
        return ioArgs;
    }

    @Override
    public boolean onConsumeFailed(Throwable e) {
        e.printStackTrace();
        CloseUtils.close(this);
        return true;
    }

    @Override
    public boolean onConsumeCompleted(IoArgs args) {
        final AtomicBoolean isClosed = this.isClosed;
        final AsyncPacketWriter writer = this.writer;
        // 消费IoArgs之前，标记为写入完成
        args.finishWriting();
        do {
            writer.consumeIoArgs(args);
        } while (args.remained() && !isClosed.get());
        return !isClosed.get();
    }

    /**
     * 构建Packet,根据类型、长度构建接收数据包
     */
    @Override
    public ReceivePacket takePacket(byte type, long length, byte[] headerInfo) {
        return receiveCallback.onArrivedNewPacket(type, length, headerInfo);
    }

    /**
     * 接收数据完成
     */
    @Override
    public void completedPacket(ReceivePacket packet, boolean isSucceed) {
        CloseUtils.close(packet);
        receiveCallback.onReceivePacketCompleted(packet);
    }

    /**
     * 接收到心跳
     */
    @Override
    public void onReceiveHeartbeat() {
        receiveCallback.onReceiveHeartbeat();
    }
}
