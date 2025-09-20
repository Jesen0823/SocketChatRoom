package org.jesen.library.clink.impl.async;

import org.jesen.library.clink.core.IoArgs;
import org.jesen.library.clink.core.ReceiveDispatcher;
import org.jesen.library.clink.core.ReceivePacket;
import org.jesen.library.clink.core.Receiver;
import org.jesen.library.clink.utils.CloseUtils;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncReceiveDispatcher implements ReceiveDispatcher, IoArgs.IoArgsEventListener, AsyncPacketWriter.PacketProvider {

    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final Receiver receiver;
    private final ReceivePacketCallback receiveCallback;
    private final AsyncPacketWriter packetWriter = new AsyncPacketWriter(this);

    public AsyncReceiveDispatcher(Receiver receiver, ReceivePacketCallback receiveCallback) {
        this.receiver = receiver;
        this.receiver.setReceiveListener(this);
        this.receiveCallback = receiveCallback;
    }

    @Override
    public void start() {
        registerReceive();
    }

    @Override
    public void stop() {
        receiver.setReceiveListener(null);
    }

    @Override
    public void close() throws IOException {
        if (isClosed.compareAndSet(false, true)) {
            packetWriter.close();
        }
    }

    private void registerReceive() {
        try {
            receiver.postReceiveAsync();
        } catch (Exception e) {
            e.printStackTrace();
            closeAndNotify();
        }
    }

    private void closeAndNotify() {
        CloseUtils.close(this);
    }

    @Override
    public IoArgs provideIoArgs() {
        IoArgs args = packetWriter.takeIoArgs();
        // 开始写入数据了
        args.startWriting();
        return args;
    }

    @Override
    public boolean onConsumeFailed(IoArgs args, Throwable e) {
        return false;
    }

    @Override
    public boolean onConsumeCompleted(IoArgs args) {
        if (isClosed.get()) {
            return false;
        }
        // 消费数据之前，标识已经完成
        args.finishWriting();
        // 有数据才消费
        do {
            packetWriter.consumeIoArgs(args);
        } while (args.remained() && !isClosed.get());

        // 接收下一次数据
        registerReceive();
        return true;
    }

    /**
     * 构建Packet,根据类型、长度构建一份用于接收数据的Packet
     */
    @Override
    public ReceivePacket takePacket(byte type, long length, byte[] headerInfo) {
        return receiveCallback.onArrivedNewPacket(type, length, headerInfo);
    }

    /**
     * 当Packet接收数据完成或终止时回调
     */
    @Override
    public void completedPacket(ReceivePacket packet, boolean isSucceed) {
        CloseUtils.close(packet);
        receiveCallback.onReceivePacketCompleted(packet);
    }

    @Override
    public void onReceivedHeartbeat() {
        receiveCallback.onReceiveHeartbeatCompleted();
    }
}
