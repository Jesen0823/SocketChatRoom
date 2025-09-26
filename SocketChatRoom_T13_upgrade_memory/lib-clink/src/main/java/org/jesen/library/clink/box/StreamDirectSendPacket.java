package org.jesen.library.clink.box;

import org.jesen.library.clink.core.Packet;
import org.jesen.library.clink.core.SendPacket;

import java.io.InputStream;

/**
 * 直流发送包 用于实时语音包
 */
public class StreamDirectSendPacket extends SendPacket<InputStream> {

    private InputStream inputStream;

    public StreamDirectSendPacket(InputStream inputStream) {
        this.inputStream = inputStream;
        // 实时语音长度不固定，因而取最大值
        this.length = Packet.MAX_PACKET_SIZE;
    }

    @Override
    public byte type() {
        return Packet.TYPE_STREAM_DIRECT;
    }

    @Override
    protected InputStream createStream() {
        return inputStream;
    }
}
