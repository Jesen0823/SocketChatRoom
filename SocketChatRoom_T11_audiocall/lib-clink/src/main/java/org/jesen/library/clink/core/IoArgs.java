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
    private int limit = 256;
    private ByteBuffer buffer = ByteBuffer.allocate(256);

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

    public int capacity() {
        return buffer.capacity();
    }

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
     * IoArgs 提供者、处理者，数据的生产或消费
     */
    public interface IoArgsEventProcessor {
        /**
         * 提供可消费的IoArgs
         */
        IoArgs provideIoArgs();

        void onConsumeFailed(IoArgs args, Exception e);

        void onConsumeCompleted(IoArgs args);
    }
}
