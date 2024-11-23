package net.qiujuer.library.clink.impl.async;

import net.qiujuer.library.clink.core.IoArgs;
import net.qiujuer.library.clink.core.ReceiveDispatcher;
import net.qiujuer.library.clink.core.ReceivePacket;
import net.qiujuer.library.clink.core.Receiver;
import net.qiujuer.library.clink.utils.CloseUtils;

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
        return packetWriter.takeIoArgs();
    }

    @Override
    public boolean onConsumeFailed(IoArgs args, Throwable e) {
        return false;
    }

    @Override
    public boolean onConsumeCompleted(IoArgs args) {
        if (isClosed.get()){
            return false;
        }
        // 有数据才消费
        do {
            packetWriter.consumeIoArgs(args);
        }while (args.remained() && !isClosed.get());

        // 接收下一次数据
        registerReceive();
        return true;
    }

    @Override
    public ReceivePacket takePacket(byte type, long length, byte[] headerInfo) {
        return receiveCallback.onArrivedNewPacket(type, length);
    }

    @Override
    public void completedPacket(ReceivePacket packet, boolean isSucceed) {
        CloseUtils.close(packet);
        receiveCallback.onReceivePacketCompleted(packet);
    }
}
