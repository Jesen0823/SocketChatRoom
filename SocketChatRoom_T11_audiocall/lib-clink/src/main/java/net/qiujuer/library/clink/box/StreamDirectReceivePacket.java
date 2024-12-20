package net.qiujuer.library.clink.box;

import net.qiujuer.library.clink.core.Packet;
import net.qiujuer.library.clink.core.ReceivePacket;

import java.io.OutputStream;

/**
 * 直流数据接收包
 */

public class StreamDirectReceivePacket extends ReceivePacket<OutputStream, OutputStream> {
    private OutputStream outputStream;

    public StreamDirectReceivePacket(OutputStream outputStream, long length) {
        super(length);
        this.outputStream = outputStream;
    }

    @Override
    public byte type() {
        return Packet.TYPE_STREAM_DIRECT;
    }

    @Override
    protected OutputStream createStream() {
        return outputStream;
    }

    @Override
    protected OutputStream buildEntity(OutputStream stream) {
        return outputStream;
    }
}
