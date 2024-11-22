package net.qiujuer.library.clink.impl.async;

import net.qiujuer.library.clink.box.StringReceivePacket;
import net.qiujuer.library.clink.core.IoArgs;
import net.qiujuer.library.clink.core.ReceiveDispatcher;
import net.qiujuer.library.clink.core.ReceivePacket;
import net.qiujuer.library.clink.core.Receiver;
import net.qiujuer.library.clink.utils.CloseUtils;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncReceiveDispatcher implements ReceiveDispatcher, IoArgs.IoArgsEventListener {

    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final Receiver receiver;
    private final ReceivePacketCallback receiveCallback;

    private IoArgs ioArgs = new IoArgs();
    private ReceivePacket<?,?> packetTemp;
    private WritableByteChannel packetChannel;
    private long total;
    private long position;

    /**
     * 解析数据到Packet
     */
    private void assemblePacket(IoArgs args) {
        if (packetTemp == null) {
            int length = args.readLength();
            packetTemp = new StringReceivePacket(length);
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
                packetTemp = null;
            }

        } catch (IOException e) {
            e.printStackTrace();
            completePacket(false);
        }
    }

    /**
     * 完成数据接收操作
     */
    private void completePacket(boolean isSucceed) {
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
            completePacket(false);
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
        IoArgs args = ioArgs;

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
    public boolean onConsumeFailed(Throwable e) {
        e.printStackTrace();
        return true;
    }

    @Override
    public boolean onConsumeCompleted(IoArgs args) {
        assemblePacket(args);
        // 接收下一次数据
        registerReceive();
        return true;
    }
}
