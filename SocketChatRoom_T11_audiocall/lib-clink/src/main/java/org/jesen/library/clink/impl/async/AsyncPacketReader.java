package org.jesen.library.clink.impl.async;

import org.jesen.library.clink.core.Frame;
import org.jesen.library.clink.core.IoArgs;
import org.jesen.library.clink.core.SendPacket;
import org.jesen.library.clink.core.ds.BytePriorityNode;
import org.jesen.library.clink.frames.HeartbeatSendFrame;
import org.jesen.library.clink.frames.base.AbsSendPacketFrame;
import org.jesen.library.clink.frames.CancelSendFrame;
import org.jesen.library.clink.frames.SendEntityFrame;
import org.jesen.library.clink.frames.SendHeaderFrame;

import java.io.Closeable;
import java.io.IOException;

/*
 * 管理分片逻辑的实现
 * 负责从Dispatcher拿数据
 * */
public class AsyncPacketReader implements Closeable {
    private volatile IoArgs ioArgs = new IoArgs();
    private final PacketProvider packetProvider;
    private volatile BytePriorityNode<Frame> node;
    private volatile int nodeSize = 0;

    // 唯一标识，自增，0~255,理论上的并发度
    private short lastIdentifier = 0;

    AsyncPacketReader(PacketProvider provider) {
        this.packetProvider = provider;
    }

    synchronized void cancel(SendPacket packet) {
        if (nodeSize == 0) {
            return;
        }

        for (BytePriorityNode<Frame> x = node, before = null; x != null; before = x, x = x.next) {
            Frame frame = x.item;
            if (frame instanceof AbsSendPacketFrame) {
                AbsSendPacketFrame packetFrame = (AbsSendPacketFrame) frame;
                if (packetFrame.getPacket() == packet) {
                    boolean canRemove = packetFrame.abort();
                    if (canRemove) {
                        removeFrame(x, before);

                        if (packetFrame instanceof SendHeaderFrame) {
                            // 如果是首帧,直接取消
                            break;
                        }
                    }

                    // 添加终止帧，通知接收方
                    CancelSendFrame cancelFrame = new CancelSendFrame(packetFrame.getBodyIdentifier());
                    appendNewFrame(cancelFrame);

                    // 意外终止，返回失败
                    packetProvider.completedPacket(packet, false);
                    break;
                }
            }
        }

    }

    /**
     * 请求从{@link #packetProvider}队列中拿一份Packet进行发送
     *
     * @return 如果当前Reader中有可发送数据，则返回true
     */
    boolean requestTackPacket() {
        synchronized (this) {
            // 有数据，不需要再拿
            if (nodeSize >= 1) {
                return true;
            }
        }
        // 拿当前Packet,先把头帧放入队列
        SendPacket packet = packetProvider.takePacket();
        if (packet != null) {
            short identifier = generateIdentifier();
            SendHeaderFrame frame = new SendHeaderFrame(identifier, packet);
            appendNewFrame(frame);
        }
        synchronized (this) {
            return nodeSize != 0;
        }
    }

    /**
     * 请求发送心跳帧
     *
     * @return 是否发送心跳
     */
    synchronized boolean requestSendHeartbeatFrame() {
        for (BytePriorityNode<Frame> x = node; x != null; x = x.next) {
            Frame frame = x.item;
            // 如果已经有一个心跳帧，不用再创建
            if (frame.getBodyType() == Frame.TYPE_COMMAND_HEARTBEAT) {
                return false;
            }
        }
        appendNewFrame(new HeartbeatSendFrame());
        return true;
    }

    /**
     * 填充数据到IoArgs中
     *
     * @return 如果当前有可用于发送的帧，则填充数据并返回，如果填充失败则返回null
     */
    IoArgs fillData() {
        Frame currentFrame = getCurrentFrame();
        if (currentFrame == null) {
            return null;
        }
        try {
            // 消费完本帧，尝试基于本帧构建后续帧
            if (currentFrame.handle(ioArgs)) {
                Frame nextFrame = currentFrame.nextFrame();
                if (nextFrame != null) {
                    appendNewFrame(nextFrame);
                } else if (currentFrame instanceof SendEntityFrame) {
                    // 末尾实体帧，通知发送完成
                    packetProvider.completedPacket(((SendEntityFrame) currentFrame).getPacket(), true);
                }
                // 从链表移除
                popCurrentFrame();
            }
            return ioArgs;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    private synchronized void appendNewFrame(Frame frame) {
        BytePriorityNode<Frame> newNode = new BytePriorityNode<>(frame);
        if (node != null) {
            // 使用优先级别添加到链表
            node.appendWithPriority(newNode);
        } else {
            node = newNode;
        }
        nodeSize++;
    }

    private synchronized void removeFrame(BytePriorityNode<Frame> removeNode, BytePriorityNode<Frame> before) {
        if (before == null) {
            node = removeNode.next;
        } else {
            before.next = removeNode.next;
        }
        nodeSize--;
        if (node == null) {
            requestTackPacket();
        }
    }

    /**
     * 关闭当前Reader,应关闭所有Frame对应的Packet
     */
    @Override
    public synchronized void close() {
        while (node != null) {
            Frame frame = node.item;
            if (frame instanceof AbsSendPacketFrame) {
                SendPacket packet = ((AbsSendPacketFrame) frame).getPacket();
                packetProvider.completedPacket(packet, false);
            }
            node = node.next;
        }
        nodeSize = 0;
        node = null;
    }

    /**
     * 从node移除当前帧
     */
    private synchronized void popCurrentFrame() {
        node = node.next;
        nodeSize--;
        if (node == null) {
            requestTackPacket();
        }
    }

    private synchronized Frame getCurrentFrame() {
        if (node == null) {
            return null;
        }
        return node.item;
    }

    private short generateIdentifier() {
        short identifier = ++lastIdentifier;
        if (identifier == 255) {
            lastIdentifier = 0;
        }
        return identifier;
    }

    interface PacketProvider {
        SendPacket takePacket();

        void completedPacket(SendPacket packet, boolean isSucceed);
    }
}
