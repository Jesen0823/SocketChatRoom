package org.jesen.library.clink.frames;

import org.jesen.library.clink.core.Frame;
import org.jesen.library.clink.core.IoArgs;
import org.jesen.library.clink.core.SendPacket;
import org.jesen.library.clink.frames.base.AbsSendPacketFrame;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

/**
 * 发送数据帧
 */
public class SendEntityFrame extends AbsSendPacketFrame {
    // 未消费的长度
    private final long unConsumeEntityLength;
    // 需要将通道channel中的数据组装成Frame
    private final ReadableByteChannel channel;

    public SendEntityFrame(short identifier, long entityLength, ReadableByteChannel channel, SendPacket packet) {
        super(
                (int) Math.min(entityLength, Frame.MAX_CAPACITY),
                Frame.TYPE_PACKET_ENTITY,
                Frame.FLAG_NONE,
                identifier,
                packet);
        // Packet总长度减去之前帧已经消费的长度，留给下一帧消费
        unConsumeEntityLength = entityLength - bodyRemaining;
        this.channel = channel;
    }

    @Override
    protected int consumeBody(IoArgs args) throws IOException {
        if (packet == null) {
            // 已终止当前帧，填充假数据
            return args.fillEmpty(bodyRemaining);
        }
        return args.readFrom(channel);
    }

    @Override
    public Frame buildNextFrame() {
        if (unConsumeEntityLength == 0) {
            System.out.println("SendEntityFrame ---buildNextFrame, unConsumeEntityLength == 0");
            return null;
        }
        return new SendEntityFrame(getBodyIdentifier(), unConsumeEntityLength, channel, packet);
    }
}
