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

public class AsyncSendDispatcher implements SendDispatcher, IoArgs.IoArgsEventListener, AsyncPacketReader.PacketProvider {
    private final Sender sender;
    private final Queue<SendPacket> queue = new ConcurrentLinkedDeque<>();
    private final AtomicBoolean isSending = new AtomicBoolean();
    private final AtomicBoolean isClosed = new AtomicBoolean();

    private final AsyncPacketReader reader = new AsyncPacketReader(this);
    private final Object queueLock = new Object();

    public AsyncSendDispatcher(Sender sender) {
        this.sender = sender;
        sender.setSendListener(this);
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
        // 锁的范围
        synchronized (queueLock) {
            packet = queue.poll();
            if (packet == null) {
                // 队列为空，取消发送状态
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
     * 发送packet完成
     */
    @Override
    public void completedPacket(SendPacket packet, boolean isSucceed) {
        System.out.println("AsyncSendDispatcher, completedPacket");
        CloseUtils.close(packet);
    }

    /**
     * 请求网络进行数据发送
     */
    private void requestSend() {
        // 真正发送数据
        try {
            sender.postSendAsync();
        } catch (Exception e) {
            e.printStackTrace();
            closeAndNotify();
        }
    }

    private void closeAndNotify() {
        CloseUtils.close(this);
    }

    @Override
    public void cancel(SendPacket packet) {
        boolean success;
        synchronized (queueLock) {
            success = queue.remove(packet);
        }
        if (success) {
            packet.cancel();
            return;
        }
        reader.cancel(packet);
    }

    @Override
    public void close() throws IOException {
        if (isClosed.compareAndSet(false, true)) {
            isSending.set(false);
            // 异常关闭导致的完成
            reader.close();
        }
    }

    @Override
    public IoArgs provideIoArgs() {
        return reader.fillData();
    }

    @Override
    public boolean onConsumeFailed(IoArgs args, Throwable e) {
        if (args != null) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean onConsumeCompleted(IoArgs args) {
        // 继续发送当前包
        if (reader.requestTackPacket()) {
            requestSend();
        }
        return false;
    }
}
