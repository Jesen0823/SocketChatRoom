package net.qiujuer.library.clink.core;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;

/**
 * IO输出输入的参数,封装ByteBuffer
 */
public class IoArgs {
    // 单次操作最大区间
    private volatile int limit;
    // 是否需要消费所有的区间(读取/写入)
    private final boolean isNeedConsumeRemaining;
    private ByteBuffer buffer;

    public IoArgs() {
        this(256);
    }

    public IoArgs(int size) {
        this(size, true);
    }

    public IoArgs(int size, boolean isNeedConsumeRemaining) {
        this.limit = size;
        this.isNeedConsumeRemaining = isNeedConsumeRemaining;
        this.buffer = ByteBuffer.allocate(size);
    }

    /**
     * 从bytes中读取数据
     */
    public int readFrom(byte[] bytes, int offset) {
        int size = Math.min(bytes.length - offset, buffer.remaining());
        buffer.put(bytes, offset, size);
        return size;
    }

    /**
     * 从bytes中读取数据
     */
    public int readFrom(byte[] bytes, int offset, int count) {
        int size = Math.min(count, buffer.remaining());
        if (size < 0) {
            return 0;
        }
        buffer.put(bytes, offset, size);
        return size;
    }

    /**
     * 写入数据到bytes中
     */
    public int writeTo(byte[] bytes, int offset) {
        int size = Math.min(bytes.length - offset, buffer.remaining());
        buffer.get(bytes, offset, size);
        return size;
    }

    /**
     * 从SocketChannel读取数据
     */
    public int readFrom(SocketChannel channel) throws IOException {
        int bytesProduced = 0;
        int len;
        do {
            len = channel.read(buffer);
            if (len < 0) {
                throw new EOFException("Cannot read any data with channel:" + channel);
            }
            bytesProduced += len;
        } while (buffer.hasRemaining() && len != 0);

        return bytesProduced;
    }

    /**
     * 从bytes中读取数据
     */
    public int readFrom(ReadableByteChannel channel) throws IOException {
        int bytesProduced = 0;
        while (buffer.hasRemaining()) {
            int len = channel.read(buffer);
            if (len < 0) {
                throw new EOFException();
            }
            bytesProduced += len;
        }
        return bytesProduced;
    }


    /**
     * 写入数据到bytes中
     */
    public int writeTo(WritableByteChannel channel) throws IOException {
        int bytesProduced = 0;
        while (buffer.hasRemaining()) {
            int len = channel.write(buffer);
            if (len < 0) {
                throw new EOFException();
            }
            bytesProduced += len;
        }
        return bytesProduced;
    }


    /**
     * 向SocketChannel写入数据
     */
    public int writeTo(SocketChannel channel) throws IOException {
        ByteBuffer buffer = this.buffer;
        int bytesProduced = 0;
        int len;
        do {
            len = channel.write(buffer);
            if (len < 0) {
                throw new EOFException("Current write any data with channel:" + channel);
            }
            bytesProduced += len;
        } while (buffer.hasRemaining() && len != 0);

        return bytesProduced;
    }

    /**
     * 设置单词写操作的容纳区间
     */
    public void limit(int l) {
        this.limit = Math.min(l, buffer.capacity());
    }

    /**
     * 开始写入数据到IoArgs
     */
    public void startWriting() {
        buffer.clear();
        // 定义容纳区间
        buffer.limit(limit);
    }

    /**
     * 完成写入数据到IoArgs
     */
    public void finishWriting() {
        // 翻转，将写操作变为读操作
        buffer.flip();
    }

    public int readLength() {
        return buffer.getInt();
    }

    /**
     * 获取当前容量
     */
    public int capacity() {
        return buffer.capacity();
    }

    /**
     * buffer的可存储区间是否大于0
     */
    public boolean remained() {
        return buffer.remaining() > 0;
    }

    /**
     * 是否需要填满或完全消费所有数据
     */
    public boolean isNeedConsumeRemaining() {
        return isNeedConsumeRemaining;
    }

    /**
     * 填充空数据
     */
    public int fillEmpty(int size) {
        int fillSize = Math.min(size, buffer.remaining());
        buffer.position(buffer.position() + fillSize);
        return fillSize;
    }

    /**
     * 清空所有数据
     */
    public int setEmpty(int size){
        return fillEmpty(size);
    }

    /**
     * 重置最大限制
     */
    public void resetLimit() {
        this.limit = buffer.capacity();
    }

    public interface IoArgsEventListener {
        /**
         * 提供一份可消费的IoArgs
         *
         * @return IoArgs
         */
        IoArgs provideIoArgs();

        /**
         * 消费失败时回调
         *
         * @param e 异常信息
         * @return 是否关闭链接，True关闭
         */
        boolean onConsumeFailed(IoArgs args, Throwable e);

        /**
         * 消费成功
         *
         * @param args IoArgs
         * @return True:直接注册下一份调度，False:无需注册
         */
        boolean onConsumeCompleted(IoArgs args);

    }
}
