package net.qiujuer.library.clink.frames;

import net.qiujuer.library.clink.core.Frame;
import net.qiujuer.library.clink.core.IoArgs;

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
