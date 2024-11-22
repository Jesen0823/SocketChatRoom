package net.qiujuer.library.clink.box;

import java.io.ByteArrayInputStream;

/**
 * 字符串发送包
 */
public class StringSendPacket extends BytesSendPacket {

    /**
     * 字符串发送时就是Byte数组，所以直接得到Byte数组，并按照Byte的方式发送
     */
    public StringSendPacket(String msg) {
        super(msg.getBytes());
    }

    @Override
    public byte type() {
        return TYPE_MEMORY_STRING;
    }

    @Override
    protected ByteArrayInputStream createStream() {
        return new ByteArrayInputStream(bytes);
    }
}
