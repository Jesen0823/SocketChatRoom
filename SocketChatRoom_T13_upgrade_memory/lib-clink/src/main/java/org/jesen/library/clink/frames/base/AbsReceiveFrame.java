package org.jesen.library.clink.frames.base;

import org.jesen.library.clink.core.Frame;
import org.jesen.library.clink.core.IoArgs;

import java.io.IOException;
/**
 * 基础接收帧
 * */
public abstract class AbsReceiveFrame extends Frame {
    // 帧体可读写区大小
    protected volatile int bodyRemaining;

    public AbsReceiveFrame(byte[] header) {
        super(header);
        bodyRemaining = getBodyLength();
    }

    @Override
    public boolean handle(IoArgs args) throws IOException {
        if (bodyRemaining == 0) {
            // 已读取所有数据
            return true;
        }
        bodyRemaining -= consumeBody(args);
        return bodyRemaining == 0;
    }

    @Override
    public Frame nextFrame() {
        return null;
    }

    @Override
    public int getConsumableLength() {
        return bodyRemaining;
    }

    protected abstract int consumeBody(IoArgs args) throws IOException;
}
