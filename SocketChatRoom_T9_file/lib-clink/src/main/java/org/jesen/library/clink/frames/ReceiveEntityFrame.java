package org.jesen.library.clink.frames;

import org.jesen.library.clink.core.IoArgs;
import org.jesen.library.clink.frames.base.AbsReceiveFrame;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

/**
 * 接收数据帧
 */
public class ReceiveEntityFrame extends AbsReceiveFrame {
    private WritableByteChannel channel;

    ReceiveEntityFrame(byte[] header) {
        super(header);
    }

    /**
     * 给帧设置通道
     */
    public void bindPacketChannel(WritableByteChannel channel) {
        this.channel = channel;
    }

    @Override
    protected int consumeBody(IoArgs args) throws IOException {
        return channel == null ? args.fillEmpty(bodyRemaining) : args.writeTo(channel);
    }

}
