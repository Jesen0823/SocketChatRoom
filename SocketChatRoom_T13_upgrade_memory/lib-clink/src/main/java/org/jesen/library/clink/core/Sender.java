package org.jesen.library.clink.core;

import java.io.Closeable;

public interface Sender extends Closeable {
    void setSendListener(IoArgs.IoArgsEventProcessor processor);

    // 异步发送
    void postSendAsync() throws Exception;

    // 获取最后发送消息时间
    long getLastWriteTime();
}
