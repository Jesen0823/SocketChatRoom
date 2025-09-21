package org.jesen.library.clink.impl.async;

import org.jesen.library.clink.box.StringReceivePacket;
import org.jesen.library.clink.core.*;
import org.jesen.library.clink.utils.CloseUtils;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncReceiveDispatcher implements ReceiveDispatcher, IoArgs.IoArgsEventProcessor {

    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final Receiver receiver;
    private final ReceivePacketCallback receiveCallback;

    private IoArgs ioArgs = new IoArgs();
    private ReceivePacket<?, ?> packetTemp;
    private WritableByteChannel packetChannel;
    private long total;
    private long position;

    public AsyncReceiveDispatcher(Receiver receiver, ReceivePacketCallback receiveCallback) {
        this.receiver = receiver;
        this.receiveCallback = receiveCallback;
        this.receiver.setReceiveListener(this);
    }

    /**
     * 解析数据到Packet
     */
    private void assemblePacket(IoArgs args) {
        if (packetTemp == null) {
            int length = args.readLength();
            byte type = length >200? Packet.TYPE_STREAM_FILE:Packet.TYPE_MEMORY_STRING;

            packetTemp =receiveCallback.onArrivedNewPacket(type,length);
            packetChannel = Channels.newChannel(packetTemp.open());
            total = length;
            position = 0;
        }
        // 数据写入buffer
        try {
            int count = args.writeTo(packetChannel);
            position += count;
            if (position == total) {
                completePacket(true);
            }

        } catch (IOException e) {
            e.printStackTrace();
            completePacket(false);
        }
    }

    /**
     * 完成数据接收操作
     */
    private void completePacket(boolean isSuccess) {
        ReceivePacket packet = this.packetTemp;
        CloseUtils.close(packet);
        packetTemp = null;

        WritableByteChannel channel = this.packetChannel;
        CloseUtils.close(channel);
        packetChannel = null;
        if (packet != null) {
            receiveCallback.onReceivePacketCompleted(packet);
        }
    }

    @Override
    public void start() {
        registerReceive();
    }


    @Override
    public void stop() {
        System.out.println("--AsyncReceiveDispatcher,stop.");
    }

    @Override
    public void close() throws IOException {
        if (isClosed.compareAndSet(false, true)) {
            completePacket(false);
        }
    }

    private void registerReceive() {
        try {
            receiver.postReceiveAsync();
        } catch (IOException e) {
            e.printStackTrace();
            closeAndNotify();
        }
    }

    private void closeAndNotify() {
        CloseUtils.close(this);
    }

    @Override
    public IoArgs provideIoArgs() {
        IoArgs args = this.ioArgs;
        int receiveSize;
        if (packetTemp == null) {
            receiveSize = 4;
        } else {
            receiveSize = (int) Math.min(total - position, args.capacity());
        }
        // 设置本次接收数据大小
        args.limit(receiveSize);
        return args;
    }

    @Override
    public void onConsumeFailed(IoArgs args, Exception e) {
        e.printStackTrace();
    }

    @Override
    public void onConsumeCompleted(IoArgs args) {
        assemblePacket(args);
        registerReceive(); // 接收下一次数据
    }
}
