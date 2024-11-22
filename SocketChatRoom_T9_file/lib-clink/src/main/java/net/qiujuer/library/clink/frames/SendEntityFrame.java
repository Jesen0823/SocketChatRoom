package net.qiujuer.library.clink.frames;

import net.qiujuer.library.clink.core.Frame;
import net.qiujuer.library.clink.core.IoArgs;
import net.qiujuer.library.clink.core.SendPacket;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

public class SendEntityFrame extends AbsSendPacketFrame {
    private final long unConsumeEntityLength;
    private final ReadableByteChannel channel;

    public SendEntityFrame(short identifier, long entityLength, ReadableByteChannel channel, SendPacket packet) {
        super((int) Math.min(entityLength, Frame.MAX_CAPACITY),
                Frame.TYPE_PACKAGE_ENTITY,
                Frame.FLAG_NONE,
                identifier,
                packet);
        unConsumeEntityLength = entityLength - bodyRemaining;
        this.channel = channel;
    }

    @Override
    protected int consumeBody(IoArgs args) throws IOException {
        if (packet == null){
            // 已终止当前帧，填充假数据
            return args.fillEmpty(bodyRemaining);
        }
        return args.readFrom(channel);
    }

    @Override
    public Frame buildNextFrame() {
        if (unConsumeEntityLength == 0) {
            return null;
        }
        return new SendEntityFrame(getBodyIdentifier(), unConsumeEntityLength, channel, packet);
    }
}
