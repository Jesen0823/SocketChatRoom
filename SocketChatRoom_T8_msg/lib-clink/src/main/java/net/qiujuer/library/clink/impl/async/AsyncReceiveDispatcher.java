package net.qiujuer.library.clink.impl.async;

import net.qiujuer.library.clink.box.StringReceivePacket;
import net.qiujuer.library.clink.core.IoArgs;
import net.qiujuer.library.clink.core.ReceiveDispatcher;
import net.qiujuer.library.clink.core.ReceivePacket;
import net.qiujuer.library.clink.core.Receiver;
import net.qiujuer.library.clink.utils.CloseUtils;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncReceiveDispatcher implements ReceiveDispatcher {

    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final Receiver receiver;
    private final ReceivePacketCallback receiveCallback;

    private IoArgs ioArgs = new IoArgs();
    private ReceivePacket packetTemp;
    private byte[] buffer;

    private int total;
    private int position;

    private IoArgs.IoArgsEventListener ioArgsEventListener = new IoArgs.IoArgsEventListener() {
        @Override
        public void onStarted(IoArgs args) {
            int receiveSize;
            if (packetTemp == null) {
                receiveSize = 4;
            } else {
                receiveSize = Math.min(total - position, args.capacity());
            }
            // 设置本次接收数据大小
            args.limit(receiveSize);
        }

        @Override
        public void onCompleted(IoArgs args) {
            assemblePacket(args);
            // 接着接收下一条消息
            registerReceive();
        }

    };

    /**
     * 解析数据到Packet
     */
    private void assemblePacket(IoArgs args) {
        if (packetTemp == null) {
            int length = args.readLength();
            packetTemp = new StringReceivePacket(length);
            buffer = new byte[length];
            total = length;
            position = 0;
        }
        // 数据写入buffer
        int count = args.writeTo(buffer, 0);
        if (count > 0) {
            packetTemp.save(buffer, count);
            position += count;
            if (position == total) {
                completePacket();
                packetTemp = null;
            }
        }
    }

    /**
     * 完成数据接收操作
     */
    private void completePacket() {
        ReceivePacket packet = this.packetTemp;
        CloseUtils.close(packet);
        receiveCallback.onReceivePacketCompleted(packet);
    }

    public AsyncReceiveDispatcher(Receiver receiver, ReceivePacketCallback receiveCallback) {
        this.receiver = receiver;
        this.receiver.setReceiveListener(ioArgsEventListener);
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
        if (isClosed.compareAndSet(false,true)){
            ReceivePacket packet = this.packetTemp;
            if (packet!=null){
                CloseUtils.close(packet);
                packetTemp = null;
            }
        }
    }

    private void registerReceive() {
        try {
            receiver.receiveAsync(ioArgs);
        } catch (IOException e) {
            e.printStackTrace();
            closeAndNotify();
        }
    }

    private void closeAndNotify() {
        CloseUtils.close(this);
    }

}
