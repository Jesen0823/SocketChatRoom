package net.qiujuer.library.clink.impl.async;

import net.qiujuer.library.clink.core.IoArgs;
import net.qiujuer.library.clink.core.SendDispatcher;
import net.qiujuer.library.clink.core.SendPacket;
import net.qiujuer.library.clink.core.Sender;
import net.qiujuer.library.clink.utils.CloseUtils;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncSendDispatcher implements SendDispatcher, IoArgs.IoArgsEventListener {
    private final Sender sender;
    private final Queue<SendPacket> queue = new ConcurrentLinkedDeque<>();
    private final AtomicBoolean isSending = new AtomicBoolean();
    private final AtomicBoolean isClosed = new AtomicBoolean();
    private SendPacket<?> packetTemp;
    private IoArgs ioArgs = new IoArgs();
    private ReadableByteChannel packetChannel;
    // 当前发送的Packet大小和进度
    private long total;
    private long position;

    public AsyncSendDispatcher(Sender sender) {
        this.sender = sender;
        sender.setSendListener(this);
    }

    @Override
    public void send(SendPacket packet) {
        queue.offer(packet);
        if (isSending.compareAndSet(false, true)) {
            sendNextPacket();
        }
    }

    private SendPacket takePacket() {
        SendPacket packet = queue.poll();
        if (packet != null && packet.isCanceled()) {
            // 已取消，不用发送
            return takePacket();
        }
        return packet;
    }

    private void sendNextPacket() {
        SendPacket temp = packetTemp;
        if (temp != null) {
            CloseUtils.close(temp);
        }
        SendPacket packet = packetTemp = takePacket();
        if (packet == null) {
            // 队列为空，设为取消状态
            isSending.set(false);
            return;
        }
        total = packet.length();
        position = 0;

        sendCurrentPacket();
    }

    private void sendCurrentPacket() {
        if (position >= total) { // 一个Packet已经发送完
            completePacket(position==total);
            sendNextPacket();
            return;
        }

        // 真正发送数据
        try {
            sender.postSendAsync();
        } catch (Exception e) {
            e.printStackTrace();
            closeAndNotify();
        }
    }

    /**
     * 发送packet完成
     */
    private void completePacket(boolean isSucceed){
        SendPacket packet = this.packetTemp;
        if (packet==null){
            return;
        }
        CloseUtils.close(packet);
        CloseUtils.close(packetChannel);

        packetTemp = null;
        packetChannel = null;
        total = 0;
        position=0;
    }

    private void closeAndNotify() {
        CloseUtils.close(this);
    }

    @Override
    public void cancel(SendPacket packet) {

    }

    @Override
    public void close() throws IOException {
        if (isClosed.compareAndSet(false,true)){
            isSending.set(false);
            // 异常关闭导致的完成
            completePacket(false);
        }
    }

    @Override
    public IoArgs provideIoArgs() {
        IoArgs args = ioArgs;
        if (packetChannel==null){
            packetChannel = Channels.newChannel(packetTemp.open());
            args.limit(4);
            args.writeLength((int) packetTemp.length());
        }else {
            args.limit((int) Math.min(args.capacity(),total-position));
            try {
                int count = args.readFrom(packetChannel);
                position+=count;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return args;
    }

    @Override
    public boolean onConsumeFailed(Throwable e) {
        e.printStackTrace();
        return false;
    }

    @Override
    public boolean onConsumeCompleted(IoArgs args) {
        // 继续发送当前包
        sendCurrentPacket();
        return false;
    }
}
