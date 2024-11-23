package net.qiujuer.library.clink.core;

import net.qiujuer.library.clink.impl.async.AsyncSendDispatcher;

import java.io.Closeable;
import java.io.IOException;

public interface Sender extends Closeable {
    // 异步发送
    boolean sendAsync(IoArgs args, IoArgs.IoArgsEventListener listener) throws IOException;

    void setSendListener(IoArgs.IoArgsEventListener listener);

    boolean postSendAsync () throws IOException;
}
