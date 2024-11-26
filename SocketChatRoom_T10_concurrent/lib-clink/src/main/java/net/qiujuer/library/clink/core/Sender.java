package net.qiujuer.library.clink.core;

import java.io.Closeable;
import java.io.IOException;

public interface Sender extends Closeable {

    void setSendListener(IoArgs.IoArgsEventListener listener);

    boolean postSendAsync() throws IOException;

    long getLastWriteTime();
}
