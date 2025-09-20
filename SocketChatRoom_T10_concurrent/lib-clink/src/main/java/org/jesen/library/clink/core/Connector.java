package org.jesen.library.clink.core;

import org.jesen.library.clink.box.BytesReceivePacket;
import org.jesen.library.clink.box.FileReceivePacket;
import org.jesen.library.clink.box.StringReceivePacket;
import org.jesen.library.clink.box.StringSendPacket;
import org.jesen.library.clink.impl.SocketChannelAdapter;
import org.jesen.library.clink.impl.async.AsyncReceiveDispatcher;
import org.jesen.library.clink.impl.async.AsyncSendDispatcher;
import org.jesen.library.clink.utils.CloseUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 连接的抽象
 */
public abstract class Connector implements Closeable, SocketChannelAdapter.OnChannelStatusChangedListener {
    protected UUID key = UUID.randomUUID();
    private SocketChannel channel;
    private Sender sender;
    private Receiver receiver;
    private SendDispatcher sendDispatcher;
    private ReceiveDispatcher receiveDispatcher;
    private final List<SchedulerJob> schedulerJobList = new ArrayList<>(4);

    private final ReceiveDispatcher.ReceivePacketCallback receivePacketCallback = new ReceiveDispatcher.ReceivePacketCallback() {
        @Override
        public ReceivePacket<?, ?> onArrivedNewPacket(byte type, long length) {
            switch (type) {
                case Packet.TYPE_MEMORY_BYTES:
                    return new BytesReceivePacket(length);
                case Packet.TYPE_MEMORY_STRING:
                    return new StringReceivePacket(length);
                case Packet.TYPE_STREAM_FILE:
                    return new FileReceivePacket(length, createNewReceiveFile());
                case Packet.TYPE_STREAM_DIRECT:
                    return new BytesReceivePacket(length);
                default:
                    throw new UnsupportedOperationException("Unsupported packet type:" + type);
            }
        }

        @Override
        public void onReceivePacketCompleted(ReceivePacket packet) {
            onReceiveNewPacket(packet);
        }

        @Override
        public void onReceiveHeartbeatCompleted() {
            System.out.println("onReceiveHeartbeatCompleted: " + key.toString() + "- [Heartbeat]");
        }
    };

    protected abstract File createNewReceiveFile();

    public void setup(SocketChannel socketChannel) throws IOException {
        this.channel = socketChannel;

        IoContext context = IoContext.get();
        SocketChannelAdapter adapter = new SocketChannelAdapter(channel, context.getIoProvider(), this);

        this.sender = adapter;
        this.receiver = adapter;

        sendDispatcher = new AsyncSendDispatcher(sender);
        receiveDispatcher = new AsyncReceiveDispatcher(receiver, receivePacketCallback);

        // 启动接收
        receiveDispatcher.start();
    }

    public void send(String msg) {
        SendPacket packet = new StringSendPacket(msg);
        sendDispatcher.send(packet);
    }

    public void send(SendPacket packet) {
        sendDispatcher.send(packet);
    }

    public void schedule(SchedulerJob job) {
        synchronized (schedulerJobList) {
            if (schedulerJobList.contains(job)) {
                return;
            }
            IoContext context = IoContext.get();
            Scheduler scheduler = context.getScheduler();
            job.schedule(scheduler);
            schedulerJobList.add(job);
        }
    }

    public void fireIdleTimeoutEvent() {
        sendDispatcher.sendHeartbeat();
    }

    public void fireExceptionCaught(Throwable throwable) {

    }

    /**
     * 获取最后一次活跃的时间点
     */
    public long getLastActiveTime() {
        // 最后发送和接收消息的最靠后的时间点
        return Math.max(sender.getLastWriteTime(), receiver.getLastReadTime());
    }

    @Override
    public void close() throws IOException {
        receiveDispatcher.close();
        sendDispatcher.close();
        sender.close();
        receiver.close();
        channel.close();
    }

    @Override
    public void onChannelClosed(SocketChannel channel) {
        synchronized (schedulerJobList) {
            for (SchedulerJob job : schedulerJobList) {
                job.unSchedule();
            }
            schedulerJobList.clear();
        }
        CloseUtils.close(this);
    }

    protected void onReceiveNewPacket(ReceivePacket packet) {
        //System.out.println(key.toString() + ": [Type:" + packet.type() + ", Length:" + packet.length() + "]");
    }

    public UUID getKey() {
        return key;
    }
}

