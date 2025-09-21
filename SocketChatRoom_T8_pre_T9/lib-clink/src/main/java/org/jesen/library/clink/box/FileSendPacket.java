package org.jesen.library.clink.box;

import org.jesen.library.clink.core.SendPacket;

import java.io.*;

public class FileSendPacket extends SendPacket<FileInputStream> {

    public FileSendPacket(File file) {
        this.length = file.length();
    }

    @Override
    protected FileInputStream createStream() {
        return null;
    }
}
