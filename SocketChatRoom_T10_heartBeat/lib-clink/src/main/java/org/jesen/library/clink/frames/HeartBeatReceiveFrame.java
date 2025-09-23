package org.jesen.library.clink.frames;

import org.jesen.library.clink.core.IoArgs;
import org.jesen.library.clink.frames.base.AbsReceiveFrame;

import java.io.IOException;

/**
 * 心跳接收帧
 */
public class HeartBeatReceiveFrame extends AbsReceiveFrame {
    static final HeartBeatReceiveFrame INSTANCE = new HeartBeatReceiveFrame();

    private HeartBeatReceiveFrame() {
        super(HeartBeatSendFrame.HEARTBEAT_DATA);
    }

    @Override
    protected int consumeBody(IoArgs args) throws IOException {
        return 0;
    }
}
