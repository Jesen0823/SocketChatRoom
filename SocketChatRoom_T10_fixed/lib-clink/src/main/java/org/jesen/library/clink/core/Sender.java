package org.jesen.library.clink.core;

import java.io.Closeable;
import java.io.IOException;

public interface Sender extends Closeable {
    void setSendListener(IoArgs.IoArgsEventProcessor processor);

    // 异步发送
    boolean postSendAsync() throws IOException;
}
