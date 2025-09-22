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

public class AsyncSendDispatcher implements SendDispatcher, IoArgs.IoArgsEventProcessor, AsyncPacketReader.PacketProvider {
    private final Sender sender;
    private final Queue<SendPacket> queue = new ConcurrentLinkedDeque<>();
    private final AtomicBoolean isSending = new AtomicBoolean();
    private final AtomicBoolean isClosed = new AtomicBoolean();
    private final AsyncPacketReader reader = new AsyncPacketReader(this);
    private final Object queueLock = new Object();

    public AsyncSendDispatcher(Sender sender) {
        this.sender = sender;
        this.sender.setSendListener(this);
    }

    @Override
    public void send(SendPacket packet) {
        synchronized (queueLock) {
            queue.offer(packet);
            if (isSending.compareAndSet(false, true)) {
                if (reader.requestTackPacket()) {
                    requestSend();
                }
            }
        }
    }

    @Override
    public SendPacket takePacket() {
        SendPacket packet;
        synchronized (queueLock) {
            packet = queue.poll();
            if (packet == null) {
                isSending.set(false);
                return null;
            }
        }
        if (packet.isCanceled()) {
            // 已取消，不用发送
            return takePacket();
        }
        return packet;
    }

    /**
     * 完成Packet发送
     */
    @Override
    public void completedPacket(SendPacket packet, boolean isSucceed) {
        CloseUtils.close(packet);
    }

    /**
     * 请求发送数据
     */
    private void requestSend() {
        // 真正发送数据,没有完成
        try {
            sender.postSendAsync();
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
        System.out.println("--cancel");
        boolean result;
        synchronized (queueLock) {
            result = queue.remove(packet);
        }
        if (result) {
            packet.cancel();
            return;
        }
        reader.cancel(packet);
    }

    @Override
    public void close() throws IOException {
        if (isClosed.compareAndSet(false, true)) {
            isSending.set(false);
            reader.close();
        }
    }

    @Override
    public IoArgs provideIoArgs() {
        return reader.fillData();
    }

    @Override
    public void onConsumeFailed(IoArgs args, Exception e) {
        if (args != null) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConsumeCompleted(IoArgs args) {
        // 继续发送当前包
        if (reader.requestTackPacket()) {
            requestSend();
        }
    }
}
