package org.jesen.library.clink.core;

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

    // 是否需要消费所有的区间（读取、写入）
    private final boolean isNeedConsumeRemaining;

    // Buffer
    private final ByteBuffer buffer;

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
    public int readFrom(byte[] bytes, int offset, int count) {
        int size = Math.min(count, buffer.remaining());
        buffer.put(bytes, offset, size);
        return size;
    }

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
    public int writeTo(byte[] bytes, int offset) {
        int size = Math.min(bytes.length - offset, buffer.remaining());
        buffer.get(bytes, offset, size);
        return size;
    }

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
     * 从SocketChannel读取数据
     */
    public int readFrom(SocketChannel channel) throws IOException {
        ByteBuffer buffer = this.buffer;
        int bytesProduced = 0;
        int len;
        do {
            len = channel.read(buffer);
            if (len < 0) {
                throw new EOFException("Cannot read any data with: " + channel);
            }
            bytesProduced += len;
        } while (buffer.hasRemaining() && len != 0);
        return bytesProduced;
    }

    /**
     * 向SocketChannel写入数据
     */
    public int writeTo(SocketChannel channel) throws IOException {
        ByteBuffer buffer = this.buffer;
        int bytesProduced = 0;
        int len;

        // 回调当前可读、可写时我们进行数据填充或者消费
        // 但是过程中可能SocketChannel资源被其他SocketChannel占用了资源
        // 那么我们应该让出当前的线程调度，让应该得到数据消费的SocketChannel的到CPU调度
        // 而不应该单纯的buffer.hasRemaining()判断
        do {
            len = channel.write(buffer);
            if (len < 0) {
                throw new EOFException("Current write any data with channel: " + channel);
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

    public void writeLength(int total) {
        startWriting();
        buffer.putInt(total);
        finishWriting();
    }

    public int readLength() {
        return buffer.getInt();
    }

    /**
     * 获取当前的容量
     */
    public int capacity() {
        return buffer.capacity();
    }

    /**
     * 是否还有数据需要消费，或者说是否还有空闲区间可以容纳
     */
    public boolean remained() {
        return buffer.remaining() > 0;
    }

    /**
     * 填充空数据，实际是移动buffer的position
     */
    public int fillEmpty(int size) {
        int fillSize = Math.min(size, buffer.remaining());
        buffer.position(buffer.position() + fillSize);
        return fillSize;
    }

    /**
     * 是否需要填满 或 完全消费所有数据
     */
    public boolean isNeedConsumeRemaining() {
        return isNeedConsumeRemaining;
    }

    /**
     * 重置最大限制
     */
    public void resetLimit() {
        this.limit = buffer.capacity();
    }

    /**
     * IoArgs 提供者、处理者，数据的生产或消费
     */
    public interface IoArgsEventProcessor {
        /**
         * 提供可消费的IoArgs
         */
        IoArgs provideIoArgs();

        boolean onConsumeFailed(Throwable e);

        /**
         * 消费完成
         *
         * @return 是否消费成功
         */
        boolean onConsumeCompleted(IoArgs args);
    }
}
