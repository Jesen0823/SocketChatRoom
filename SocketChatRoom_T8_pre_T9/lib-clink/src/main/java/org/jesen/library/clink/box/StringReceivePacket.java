package org.jesen.library.clink.box;

import org.jesen.library.clink.core.ReceivePacket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class StringReceivePacket extends ReceivePacket<ByteArrayOutputStream> {
    private String msg;
    public StringReceivePacket(int len) {
        length = len;
    }

    public String string() {
        return msg;
    }

    @Override
    protected void closeStream(ByteArrayOutputStream stream) throws IOException {
        super.closeStream(stream);
        msg = new String(stream.toByteArray());
    }

    @Override
    protected ByteArrayOutputStream createStream() {
        return new ByteArrayOutputStream((int) length);
    }

}
