package net.qiujuer.library.clink.box;

import net.qiujuer.library.clink.core.ReceivePacket;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * 文件接收包
 */
public class FileReceivePacket extends ReceivePacket<FileOutputStream, File> {
    private final File file;

    public FileReceivePacket(long len, File file) {
        super(len);
        this.file = file;
    }

    /**
     * 从流转变为对应实体，返回创建时传入的File
     *
     * @param stream 文件输入流
     */
    @Override
    protected File buildEntity(FileOutputStream stream) {
        return file;
    }

    @Override
    public byte type() {
        return TYPE_STREAM_FILE;
    }

    @Override
    protected FileOutputStream createStream() {
        try {
            return new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}
