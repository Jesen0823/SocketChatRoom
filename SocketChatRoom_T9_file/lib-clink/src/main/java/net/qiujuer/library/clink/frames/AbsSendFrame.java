package net.qiujuer.library.clink.frames;

import net.qiujuer.library.clink.core.Frame;
import net.qiujuer.library.clink.core.IoArgs;

import java.io.IOException;

public abstract class AbsSendFrame extends Frame {
    volatile byte headerRemaining = Frame.FRAME_HEADER_LENGTH;
    volatile int bodyRemaining;

    public AbsSendFrame(int length, byte type, byte flag, short identifier) {
        super(length, type, flag, identifier);
        bodyRemaining = length;
    }

    @Override
    public synchronized boolean handle(IoArgs args) throws IOException {
        try {
            args.limit(headerRemaining + bodyRemaining);
            args.startWriting();

            if (headerRemaining > 0 && args.remained()) {
                headerRemaining -= consumeHeader(args);
            }
            if (headerRemaining == 0 && args.remained() && bodyRemaining > 0) {
                bodyRemaining -= consumeBody(args);
            }
            // 头部和内容都消费完全
            return headerRemaining == 0 && bodyRemaining == 0;
        } finally {
            args.finishWriting();
        }
    }

    @Override
    public int getConsumableLength() {
        return headerRemaining+bodyRemaining;
    }

    /**
     * 消费Body
     */
    protected abstract int consumeBody(IoArgs args) throws IOException;

    protected synchronized boolean isSending() {
        return headerRemaining < Frame.FRAME_HEADER_LENGTH;
    }

    /**
     * 消费头部
     */
    protected byte consumeHeader(IoArgs args) throws IOException {
        int count = headerRemaining;
        int offset = header.length - count;
        return (byte) args.readFrom(header, offset, count);
    }
}
