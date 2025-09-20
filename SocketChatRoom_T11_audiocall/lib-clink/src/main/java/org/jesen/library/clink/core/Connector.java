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
    private final List<SchedulerJob> schedulerJobList = new ArrayList<>(4);

    private volatile Connector bridgeConnector;

    private final ReceiveDispatcher.ReceivePacketCallback receivePacketCallback = new ReceiveDispatcher.ReceivePacketCallback() {
        @Override
        public ReceivePacket<?, ?> onArrivedNewPacket(byte type, long length, byte[] headerInfo) {
            switch (type) {
                case Packet.TYPE_MEMORY_BYTES:
                    return new BytesReceivePacket(length);
                case Packet.TYPE_MEMORY_STRING:
                    return new StringReceivePacket(length);
                case Packet.TYPE_STREAM_FILE:
                    return new FileReceivePacket(length, createNewReceiveFile(length, headerInfo));
                case Packet.TYPE_STREAM_DIRECT:
                    return new StreamDirectReceivePacket(createNewReceiveDirectOutputStream(length, headerInfo), length);
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

    /**
     * 当接收包是文件时，需要得到一份空的文件用以数据存储
     *
     * @param length     长度
     * @param headerInfo 额外信息
     * @return 新的文件
     */
    protected abstract File createNewReceiveFile(long length, byte[] headerInfo);

    /**
     * 当接收包是直流数据包时，需要得到一个用以存储当前直流数据的输出流，
     * 所有接收到的数据都将通过输出流输出
     *
     * @param length     长度
     * @param headerInfo 额外信息
     * @return 输出流
     */
    protected abstract OutputStream createNewReceiveDirectOutputStream(long length, byte[] headerInfo);


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
     * 改变当前调度器为桥接模式
     */
    public void changeToBridge() {
        if (receiveDispatcher instanceof BridgeSocketDispatcher) {
            return;
        }
        // 老的数据接收停止
        receiveDispatcher.stop();
        // 构建新的接收者调度器
        BridgeSocketDispatcher dispatcher = new BridgeSocketDispatcher(receiver);
        receiveDispatcher = dispatcher;
        // 启动调度
        dispatcher.start();
    }

    /**
     * 将另一个链接的发送者，绑定当前链接的桥接调度器上，从而实现两个连接的桥接功能
     */
    public void bindToBridge(Sender sender) {
        if (sender == this.sender) {
            throw new UnsupportedOperationException("Can not set current connector sender.");
        }
        if (!(receiveDispatcher instanceof BridgeSocketDispatcher)) {
            throw new IllegalStateException("receiveDispatcher is not BridgeSocketDispatcher.");
        }
        ((BridgeSocketDispatcher) receiveDispatcher).bindSender(sender);
    }

    /**
     * 将之前连接的发送者解除绑定，解除桥接数据发送功能
     */
    public void unBindToBridge() {
        if (!(receiveDispatcher instanceof BridgeSocketDispatcher)) {
            throw new IllegalStateException("receiveDispatcher is not BridgeSocketDispatcher.");
        }
        ((BridgeSocketDispatcher) receiveDispatcher).bindSender(null);
    }

    /**
     * 获取当前链接的发送者
     */
    public Sender getSender() {
        return sender;
    }

    /**
     * 将之前链接的发送者解除绑定，解除桥接数据发送功能
     */
    public void relieveBridge() {
        final Connector another = this.bridgeConnector;
        if (another == null) {
            return;
        }

        this.bridgeConnector = null;
        another.bridgeConnector = null;

        if (!(this.receiveDispatcher instanceof BridgeSocketDispatcher) ||
                !(another.receiveDispatcher instanceof BridgeSocketDispatcher)) {
            throw new IllegalStateException("receiveDispatcher is not BridgeSocketDispatcher!");
        }

        this.receiveDispatcher.stop();
        another.receiveDispatcher.stop();

        this.sendDispatcher = new AsyncSendDispatcher(sender);
        this.receiveDispatcher = new AsyncReceiveDispatcher(receiver, receivePacketCallback);
        this.receiveDispatcher.start();

        another.sendDispatcher = new AsyncSendDispatcher(sender);
        another.receiveDispatcher = new AsyncReceiveDispatcher(receiver, receivePacketCallback);
        another.receiveDispatcher.start();
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

