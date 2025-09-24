package org.jesen.library.clink.core;

import java.io.Closeable;
import java.io.IOException;

public interface Receiver extends Closeable {
    void setReceiveListener(IoArgs.IoArgsEventProcessor processor);
    boolean postReceiveAsync() throws IOException;

    // 获取最后接收消息时间
    long getLastReadTime();
}
