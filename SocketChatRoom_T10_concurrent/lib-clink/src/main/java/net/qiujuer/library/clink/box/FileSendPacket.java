package net.qiujuer.library.clink.box;

import net.qiujuer.library.clink.core.SendPacket;
import net.qiujuer.library.clink.utils.CloseUtils;

import java.io.*;

/**
 * 文件发送包
 */
public class FileSendPacket extends SendPacket<FileInputStream> {

    private final File file;

    public FileSendPacket(File file) {
        this.file = file;
        this.length = file.length();
    }

    @Override
    public byte type() {
        return TYPE_STREAM_FILE;
    }

    /**
     * 使用File构建文件流，用来读取本地文件进行流的发送
     *
     * @return 文件读取流
     */
    @Override
    protected FileInputStream createStream() {
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}
