package org.jesen.library.clink.core;

import java.io.Closeable;

public interface Receiver extends Closeable {
    void setReceiveListener(IoArgs.IoArgsEventProcessor processor);
    void postReceiveAsync() throws Exception;

    // 获取最后接收消息时间
    long getLastReadTime();
}
