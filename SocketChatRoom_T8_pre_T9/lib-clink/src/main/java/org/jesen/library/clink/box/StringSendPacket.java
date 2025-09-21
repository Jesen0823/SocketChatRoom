package org.jesen.library.clink.box;

import org.jesen.library.clink.core.SendPacket;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class StringSendPacket extends SendPacket<ByteArrayInputStream> {
    private final byte[] bytes;
    private InputStream stream;

    public StringSendPacket(String msg) {
        this.bytes = msg.getBytes();
        this.length = bytes.length;
    }

    @Override
    protected ByteArrayInputStream createStream() {
        return new ByteArrayInputStream(bytes);
    }
}
