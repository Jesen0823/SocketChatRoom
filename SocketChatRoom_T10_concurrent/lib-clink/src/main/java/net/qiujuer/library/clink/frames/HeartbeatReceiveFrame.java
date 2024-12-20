package net.qiujuer.library.clink.frames;

import net.qiujuer.library.clink.core.IoArgs;

import java.io.IOException;

/**
 * 心跳接收帧
 */
public class HeartbeatReceiveFrame extends AbsReceiveFrame {
    static final HeartbeatReceiveFrame INSTANCE = new HeartbeatReceiveFrame();

    private HeartbeatReceiveFrame() {
        super(HeartbeatSendFrame.HEARTBEAT_DATA);
    }

    @Override
    protected int consumeBody(IoArgs args) throws IOException {
        return 0;
    }
}
