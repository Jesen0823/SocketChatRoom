package org.jesen.library.clink.impl.async;

import org.jesen.library.clink.core.IoArgs;
import org.jesen.library.clink.core.SendDispatcher;
import org.jesen.library.clink.core.SendPacket;
import org.jesen.library.clink.core.Sender;
import org.jesen.library.clink.utils.CloseUtils;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncSendDispatcher implements SendDispatcher {
    private final Sender sender;
    private final Queue<SendPacket>  queue = new ConcurrentLinkedDeque<>();
    private final AtomicBoolean isSending = new AtomicBoolean();
    private final AtomicBoolean isClosed = new AtomicBoolean();
    private SendPacket packetTemp;
    private IoArgs ioArgs = new IoArgs();

    private IoArgs.IoArgsEventListener ioArgsEventListener = new IoArgs.IoArgsEventListener() {
        @Override
        public void onStarted(IoArgs args) {

        }

        @Override
        public void onCompleted(IoArgs args) {
            // 继续发送当前包
            sendCurrentPacket();
        }
    };

    // 当前发送的Packet大小和进度
    private int total;
    private int position;

    public AsyncSendDispatcher(Sender sender) {
        this.sender = sender;
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
        IoArgs args = ioArgs;

        args.startWriting();
        if (position >= total) { // 一个Packet已经发送完
            sendNextPacket();
            return;
        } else if (position == 0) { // 刚开始发送
            // 首包，需要携带长度信息
            args.writeLength(total);
        }
        byte[] bytes = packetTemp.bytes();
        // 把bytes数据写入IoArgs
        int count = args.readFrom(bytes, position);
        position += count;
        args.finishWriting();

        // 真正发送数据
        try {
            sender.sendAsync(args,ioArgsEventListener);
        } catch (IOException e) {
            e.printStackTrace();
            closeAndNotify();
        }
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
            SendPacket packet = packetTemp;
            if(packet!=null){
                packetTemp = null;
                CloseUtils.close(packet);
            }
        }
    }
}
