package net.qiujuer.library.clink.box;

import java.io.ByteArrayOutputStream;

/**
 * 字符串接收包
 */
public class StringReceivePacket extends AbsByteArrayReceivePacket<String> {

    public StringReceivePacket(long len) {
        super(len);
    }

    @Override
    public byte type() {
        return TYPE_MEMORY_STRING;
    }

    @Override
    protected String buildEntity(ByteArrayOutputStream stream) {
        return new String(stream.toByteArray());
    }
}
