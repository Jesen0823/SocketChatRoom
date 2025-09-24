package org.jesen.library.clink.core;

import java.io.Closeable;
import java.io.IOException;

public interface Sender extends Closeable {
    void setSendListener(IoArgs.IoArgsEventProcessor processor);

    // 异步发送
    boolean postSendAsync() throws IOException;

    // 获取最后发送消息时间
    long getLastWriteTime();
}
