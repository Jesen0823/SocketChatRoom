package org.jesen.library.clink.core;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 接受包
 */
public abstract class ReceivePacket<T extends OutputStream, E> extends Packet<T> {
    // 当前接收包最终的实体
    private E entity;

    public ReceivePacket(long len) {
        this.length = len;
    }

    /**
     * 得到最终接收的数据实体
     *
     * @return 数据实体
     */
    public E entity() {
        return entity;
    }

    /**
     * 根据接收到的流转化为对应的实体
     *
     * @param stream {@link OutputStream}
     * @return 实体
     */
    protected abstract E buildEntity(T stream);

    /**
     * 先关闭流，接着将流转化为对应实体
     *
     * @param stream 待关闭的流
     */
    @Override
    protected final void closeStream(T stream) throws IOException {
        super.closeStream(stream);
        entity = buildEntity(stream);
    }
}
