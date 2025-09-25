package org.jesen.library.clink.impl.stealing;

import org.jesen.library.clink.core.IoProvider;

import java.nio.channels.SocketChannel;

/**
 * 可调度任务的封装
 * 任务执行的回调、当前任务类型，任务对应通道
 */
public class IoTask {
    public final SocketChannel channel;
    public final IoProvider.HandleProviderCallback providerCallback;
    public final int ops;

    public IoTask(SocketChannel channel, IoProvider.HandleProviderCallback providerCallback, int ops) {
        this.channel = channel;
        this.providerCallback = providerCallback;
        this.ops = ops;
    }
}
