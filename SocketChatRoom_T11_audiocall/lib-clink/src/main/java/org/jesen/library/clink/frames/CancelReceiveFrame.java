package org.jesen.library.clink.frames;

import org.jesen.library.clink.core.IoArgs;
import org.jesen.library.clink.frames.base.AbsReceiveFrame;

import java.io.IOException;

/**
 * 取消传输帧
 */
public class CancelReceiveFrame extends AbsReceiveFrame {
    public CancelReceiveFrame(byte[] header) {
        super(header);
    }

    @Override
    protected int consumeBody(IoArgs args) throws IOException {
        return 0;
    }
}
