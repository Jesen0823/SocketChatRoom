package org.jesen.library.clink.core;

import java.io.Closeable;

/**
 * 发送数据调度者
 * 缓存需要发送的数据，通过队列对待发数据进行发送
 * 发送数据时做一个包装
 */
public interface SendDispatcher extends Closeable{

    void send(SendPacket packet);

    void cancel(SendPacket packet);

    void sendHeartbeat();
}
