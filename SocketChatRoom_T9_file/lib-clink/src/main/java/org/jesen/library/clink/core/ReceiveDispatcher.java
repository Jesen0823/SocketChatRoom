package org.jesen.library.clink.core;

import java.io.Closeable;

/**
 * 接收数据的调度者
 * 把一份或多份IoArgs组合成一份Packet
 */
public interface ReceiveDispatcher extends Closeable {

    /**
     * 开始接收消息
     */
    void start();

    /**
     * 结束接收消息
     */
    void stop();

    interface ReceivePacketCallback {
        ReceivePacket<?, ?> onArrivedNewPacket(byte type, long length);

        void onReceivePacketCompleted(ReceivePacket packet);
    }
}
