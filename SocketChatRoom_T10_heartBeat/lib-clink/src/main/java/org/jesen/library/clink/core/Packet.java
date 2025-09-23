package org.jesen.library.clink.core;

import java.io.Closeable;
import java.io.IOException;

/**
 * 公共数据封装
 * 提供了类型与长度定义
 */
public abstract class Packet<S extends Closeable> implements Closeable {
    // BYTES 类型
    public static final byte TYPE_MEMORY_BYTES = 1;
    // String 类型
    public static final byte TYPE_MEMORY_STRING = 2;
    // 文件 类型
    public static final byte TYPE_STREAM_FILE = 3;
    // 长链接流 类型
    public static final byte TYPE_STREAM_DIRECT = 4;

    protected long length;
    private S stream;

    /**
     * 创建流，将当前数据转换为流
     *
     * @return {@link java.io.InputStream}
     * or {@link java.io.ByteArrayInputStream}
     * or {@link java.io.FileInputStream}
     */
    protected abstract S createStream();

    public long length() {
        return length;
    }

    /**
     * 类型，直接通过方法得到
     * <p>
     * {@link #TYPE_MEMORY_BYTES}
     * {@link #TYPE_MEMORY_STRING}
     * {@link #TYPE_STREAM_DIRECT}
     * {@link #TYPE_STREAM_FILE}
     *
     * @return 类型
     */
    public abstract byte type();

    /**
     * 获取流
     */
    public final S open() {
        if (stream == null) {
            stream = createStream();
        }
        return stream;
    }

    @Override
    public final void close() throws IOException {
        if (stream != null) {
            closeStream(stream);
            stream = null;
        }
    }

    protected void closeStream(S stream) throws IOException {
        if (stream != null) {
            stream.close();
        }
    }

    /**
     * 头部额外信息，携带额外校验信息
     */
    public byte[] headerInfo(){
        return null;
    }

}
