package org.jesen.library.clink.core;

import java.nio.channels.SocketChannel;

/**
 * 可调度任务的封装
 * 任务执行的回调、当前任务类型，任务对应通道
 */
public abstract class IoTask {
    public final SocketChannel channel;
    public final int ops;

    public IoTask(SocketChannel channel, int ops) {
        this.channel = channel;
        this.ops = ops;
    }

    public abstract boolean onProcess();

    public abstract void fireThrowable(Throwable e);
}
