package net.qiujuer.library.clink.impl.async;

import net.qiujuer.library.clink.core.IoArgs;
import net.qiujuer.library.clink.core.SendDispatcher;
import net.qiujuer.library.clink.core.SendPacket;
import net.qiujuer.library.clink.core.Sender;
import net.qiujuer.library.clink.utils.CloseUtils;

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

    public AsyncSendDispatcher(Sender sender) {
        this.sender = sender;
        sender.setSendListener(this);
    }


    @Override
    public void sendHeartbeat() {
        // 已经有业务数据在发送，没必要发心跳帧去探测连接状态
        if (queue.size() > 0) {
            return;
        }
        if (reader.requestSendHeartbeatFrame()){
            requestSend();
        }
    }

    @Override
    public void send(SendPacket packet) {
        queue.offer(packet);
        requestSend();
    }

    @Override
    public SendPacket takePacket() {
        SendPacket packet = queue.poll();
        if (packet == null) {
            return null;
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
        CloseUtils.close(packet);
    }

    /**
     * 请求网络进行数据发送
     */
    private void requestSend() {
        synchronized (isSending) {
            if (isSending.get() || isClosed.get()) {
                return;
            }
            // 如果有数据要发送
            if (reader.requestTackPacket()) {
                try {
                    boolean isSucceed = sender.postSendAsync();
                    if (isSucceed) {
                        isSending.set(true);
                    }
                } catch (IOException e) {
                    closeAndNotify();
                }
            }
        }
    }

    private void closeAndNotify() {
        CloseUtils.close(this);
    }

    @Override
    public void cancel(SendPacket packet) {
        boolean success = queue.remove(packet);
        if (success) {
            packet.cancel();
            return;
        }
        reader.cancel(packet);
    }

    @Override
    public void close() throws IOException {
        if (isClosed.compareAndSet(false, true)) {
            // 异常关闭导致的完成
            reader.close();
            queue.clear();
            synchronized (isSending) {
                isSending.set(false);
            }
        }
    }

    @Override
    public IoArgs provideIoArgs() {
        return isClosed.get() ? null : reader.fillData();
    }

    @Override
    public boolean onConsumeFailed(IoArgs args, Throwable e) {
        e.printStackTrace();
        synchronized (isSending) {
            isSending.set(false);
        }
        // 继续请求发送当前数据
        requestSend();
        return false;
    }

    @Override
    public boolean onConsumeCompleted(IoArgs args) {
        // 继续发送当前包
        synchronized (isSending) {
            isSending.set(false);
        }
        // 继续请求发送当前数据
        requestSend();
        return false;
    }
}
