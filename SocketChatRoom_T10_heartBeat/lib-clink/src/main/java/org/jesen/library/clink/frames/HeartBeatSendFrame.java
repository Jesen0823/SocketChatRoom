package org.jesen.library.clink.frames;

import org.jesen.library.clink.core.Frame;
import org.jesen.library.clink.core.IoArgs;
import org.jesen.library.clink.frames.base.AbsSendFrame;

/**
 * 心跳发送帧
 */
public class HeartBeatSendFrame extends AbsSendFrame {

    // 心跳帧的数据固定不变
    static final byte[] HEARTBEAT_DATA = new byte[]{0,0,Frame.TYPE_COMMAND_HEARTBEAT,0,0,0};
    public HeartBeatSendFrame() {
        super(HEARTBEAT_DATA);
    }

    @Override
    protected int consumeBody(IoArgs args) {
        return 0;
    }

    @Override
    public Frame nextFrame() {
        return null;
    }
}
