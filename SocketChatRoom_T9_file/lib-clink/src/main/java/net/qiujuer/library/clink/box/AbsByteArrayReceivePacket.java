package net.qiujuer.library.clink.box;

import net.qiujuer.library.clink.core.ReceivePacket;

import java.io.ByteArrayOutputStream;

/**
 * 定义基础的基于{@link ByteArrayOutputStream} 的数据接收包
 *
 * @param <E> 对应的实体泛型，定义{@link ByteArrayOutputStream} 流最终转化的数据实体
 */
public abstract class AbsByteArrayReceivePacket<E> extends ReceivePacket<ByteArrayOutputStream, E> {

    public AbsByteArrayReceivePacket(long len) {
        super(len);
    }

    /**
     * 创建流
     */
    @Override
    protected ByteArrayOutputStream createStream() {
        return new ByteArrayOutputStream((int) length);
    }
}
