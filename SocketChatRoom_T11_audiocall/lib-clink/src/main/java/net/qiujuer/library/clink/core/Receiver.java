package net.qiujuer.library.clink.core;

import java.io.Closeable;
import java.io.IOException;

public interface Receiver extends Closeable {
    void setReceiveListener(IoArgs.IoArgsEventListener listener);

    /**
     * 注册失败则抛出异常
     *
     * @throws Exception 异常信息
     */
    boolean postReceiveAsync() throws Exception;

    /**
     * 获取读取数据的时间
     *
     * @return 毫秒
     */
    long getLastReadTime();

}
