package org.jesen.library.clink.impl.async;

import org.jesen.library.clink.core.IoArgs;
import org.jesen.library.clink.core.SendDispatcher;
import org.jesen.library.clink.core.SendPacket;
import org.jesen.library.clink.core.Sender;
import org.jesen.library.clink.impl.exceptions.EmptyIoArgsException;
import org.jesen.library.clink.utils.CloseUtils;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncSendDispatcher implements SendDispatcher, IoArgs.IoArgsEventProcessor, AsyncPacketReader.PacketProvider {
    private final Sender sender;
    private final BlockingQueue<SendPacket> queue = new ArrayBlockingQueue<>(16);
    private final AtomicBoolean isSending = new AtomicBoolean(false);
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final AsyncPacketReader reader = new AsyncPacketReader(this);

    public AsyncSendDispatcher(Sender sender) {
        this.sender = sender;
        this.sender.setSendListener(this);
    }

    /**
     * 如果数据生产者太快，则队列很容易爆满
     * 所以queue应该用阻塞队列
     */
    @Override
    public void send(SendPacket packet) {
        try {
            queue.put(packet);
            requestSend(false);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendHeartbeat() {
        // 有数据在排队发送，则没必要发心跳
        if (!queue.isEmpty()) {
            return;
        }
        if (reader.requestSendHeartbeatFrame()) {
            requestSend(false);
            System.out.println("---AsyncSendDispatcher, send a heart!");
        }
    }

    /**
     * reader从当前队列中提取一份Packet
     *
     * @return 如果队列有可用于发送的数据则返回该Packet
     */
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
     * 完成Packet发送
     *
     * @param isSucceed 是否成功
     */
    @Override
    public void completedPacket(SendPacket packet, boolean isSucceed) {
        CloseUtils.close(packet);
    }

    /**
     * 请求发送数据
     *
     * @param fromConsume 是否来自IO消费调度
     */
    private void requestSend(boolean fromConsume) {
        // 真正发送数据,没有完成
        synchronized (isSending) {
            final AtomicBoolean isRegisterSend = this.isSending;
            final boolean lastState = isRegisterSend.get();
            if (isClosed.get() || (lastState && !fromConsume)) {
                return;
            }
            if (fromConsume && !lastState) {
                throw new IllegalStateException("Call from IoConsume, current state should in sending!");
            }
            // 返回true代表有数据要发送
            if (reader.requestTackPacket()) {
                isRegisterSend.set(true);
                try {
                    sender.postSendAsync();
                } catch (Exception e) {
                    e.printStackTrace();
                    CloseUtils.close(this);
                }
            } else {
                isRegisterSend.set(false);
            }
        }
    }

    @Override
    public void cancel(SendPacket packet) {
        System.out.println("--cancel");
        boolean result = queue.remove(packet);
        if (result) {
            packet.cancel();
            return;
        }
        reader.cancel(packet);
    }

    @Override
    public void close() throws IOException {
        if (isClosed.compareAndSet(false, true)) {
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
    public boolean onConsumeFailed(Throwable e) {
        if (e instanceof EmptyIoArgsException) {
            requestSend(true);
            return false;
        } else {
            CloseUtils.close(this);
            return true;
        }
    }

    @Override
    public boolean onConsumeCompleted(IoArgs args) {
        synchronized (isSending) {
            AtomicBoolean isRegisterSend = this.isSending;
            final boolean isRunning = !isClosed.get();
            // 正在运行当中
            if (!isRegisterSend.get() && isRunning) {
                throw new IllegalStateException("Call from IoConsume, current state should in sending.");
            }
            isRegisterSend.set(isRunning && reader.requestTackPacket());
            return isRegisterSend.get();
        }
    }
}
