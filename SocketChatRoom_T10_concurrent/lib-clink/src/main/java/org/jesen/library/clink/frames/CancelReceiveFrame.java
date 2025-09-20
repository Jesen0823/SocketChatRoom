package org.jesen.library.clink.frames;

import org.jesen.library.clink.core.IoArgs;

import java.io.IOException;

public class CancelReceiveFrame extends AbsReceiveFrame {
    public CancelReceiveFrame(byte[] header) {
        super(header);
    }

    @Override
    protected int consumeBody(IoArgs args) throws IOException {
        return 0;
    }
}
