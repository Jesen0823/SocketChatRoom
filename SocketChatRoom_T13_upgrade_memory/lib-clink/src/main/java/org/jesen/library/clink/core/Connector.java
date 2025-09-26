package org.jesen.library.clink.core;

import org.jesen.library.clink.box.*;
import org.jesen.library.clink.impl.SocketChannelAdapter;
import org.jesen.library.clink.impl.async.AsyncReceiveDispatcher;
import org.jesen.library.clink.impl.async.AsyncSendDispatcher;
import org.jesen.library.clink.impl.bridge.BridgeSocketDispatcher;
import org.jesen.library.clink.utils.CloseUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
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
    private final List<ScheduleJob> scheduleJobs = new ArrayList<>(4);

    // 数据接收到的回调
    private ReceiveDispatcher.ReceivePacketCallback receivePacketCallback = new ReceiveDispatcher.ReceivePacketCallback() {
        @Override
        public ReceivePacket<?, ?> onArrivedNewPacket(byte type, long length,byte[] headInfo) {
            switch (type) {
                case Packet.TYPE_MEMORY_BYTES:
                    return new BytesReceivePacket(length);
                case Packet.TYPE_MEMORY_STRING:
                    return new StringReceivePacket(length);
                case Packet.TYPE_STREAM_FILE:
                    return new FileReceivePacket(length, createNewReceiveFile(length,headInfo));
                case Packet.TYPE_STREAM_DIRECT:
                    return new StreamDirectReceivePacket(createNewReceiveDirectOutputStream(length,headInfo),length);
                default:
                    throw new UnsupportedOperationException("Unsupported packet type:" + type);
            }
        }

        @Override
        public void onReceivePacketCompleted(ReceivePacket packet) {
            onReceiveNewPacket(packet);
        }

        @Override
        public void onReceiveHeartbeat() {
            System.out.println(key.toString() + ": [Heartbeat]");
        }
    };

    protected abstract OutputStream createNewReceiveDirectOutputStream(long length, byte[] headInfo);

    protected abstract File createNewReceiveFile(long length,byte[] headInfo);

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

    public void schedule(ScheduleJob job) {
        synchronized (scheduleJobs) {
            if (scheduleJobs.contains(job)) {
                return;
            }
            IoContext context = IoContext.get();
            Scheduler scheduler = context.getScheduler();
            job.schedule(scheduler);
            scheduleJobs.add(job);
        }
    }

    public void fireIdleTimeoutEvent() {
        sendDispatcher.sendHeartbeat();
    }

    public void fireExceptionCaught(Throwable throwable) {
    }

    /**
     * 获取最后活跃时间点
     */
    public long getLastActiveTime() {
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
        synchronized (scheduleJobs) {
            for (ScheduleJob job : scheduleJobs) {
                job.unSchedule();
            }
            scheduleJobs.clear();
        }
        CloseUtils.close(this);
    }

    protected void onReceiveNewMessage(String str) {
        //System.out.println("Connector onReceiveNewMessage(): " + key.toString() + ":" + str);
    }

    protected void onReceiveNewPacket(ReceivePacket packet) {
        //System.out.println("--onReceiveNewPacket");
        System.out.println("onReceiveNewPacket() " + key.toString() + ": [Type:" + packet.type() + ", Length:" + packet.length() + "]");
    }

    public UUID getKey() {
        return key;
    }

    /**
     * 改变当前调度器为桥接模式
     */
    public void changeToBridge() {
        if (receiveDispatcher instanceof BridgeSocketDispatcher) {
            // 已改变直接返回
            return;
        }

        // 老的停止
        receiveDispatcher.stop();

        // 构建新的接收者调度器
        BridgeSocketDispatcher dispatcher = new BridgeSocketDispatcher(receiver);
        receiveDispatcher = dispatcher;
        // 启动
        dispatcher.start();
    }

    /**
     * 将另外一个链接的发送者绑定到当前链接的桥接调度器上实现两个链接的桥接功能
     *
     * @param sender 另外一个链接的发送者
     */
    public void bindToBridge(Sender sender) {
        if (sender == this.sender) {
            throw new UnsupportedOperationException("Can not set current connector sender to self bridge mode!");
        }

        if (!(receiveDispatcher instanceof BridgeSocketDispatcher)) {
            throw new IllegalStateException("ReceiveDispatcher is not BridgeSocketDispatcher!");
        }

        ((BridgeSocketDispatcher) receiveDispatcher).bindSender(sender);
    }

    /**
     * 将之前链接的发送者解除绑定，解除桥接数据发送功能
     */
    public void unBindToBridge() {
        if (!(receiveDispatcher instanceof BridgeSocketDispatcher)) {
            throw new IllegalStateException("ReceiveDispatcher is not BridgeSocketDispatcher!");
        }

        ((BridgeSocketDispatcher) receiveDispatcher).bindSender(null);
    }

    /**
     * 获取当前链接的发送者
     *
     * @return 发送者
     */
    public Sender getSender() {
        return sender;
    }
}

