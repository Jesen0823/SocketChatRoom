package org.jesen.library.clink.frames.base;

import org.jesen.library.clink.core.Frame;
import org.jesen.library.clink.core.IoArgs;

import java.io.IOException;

/**
 * 基础发送帧
 */
public abstract class AbsSendFrame extends Frame {

    volatile byte headerRemaining = Frame.FRAME_HEADER_LENGTH;
    protected volatile int bodyRemaining;

    protected AbsSendFrame(int length, byte type, byte flag, short identifier) {
        super(length, type, flag, identifier);
        bodyRemaining = length;
    }

    protected AbsSendFrame(byte[] header) {
        super(header);
    }

    @Override
    public synchronized boolean handle(IoArgs args) throws IOException {
        try {
            args.limit(headerRemaining + bodyRemaining); // 设置可读区间
            args.startWriting();

            // 还有头部数据可以消费
            if (headerRemaining > 0 && args.remained()) {
                headerRemaining -= consumeHeader(args);
            }
            // 头部消费完成，消费body数据
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
        return headerRemaining + bodyRemaining;
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
