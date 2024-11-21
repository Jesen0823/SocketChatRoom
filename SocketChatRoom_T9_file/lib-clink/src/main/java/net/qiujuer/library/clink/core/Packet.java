package net.qiujuer.library.clink.core;

import java.io.Closeable;
import java.io.IOException;

/**
 * 公共数据封装
 * 提供了类型与长度定义
 * */
public abstract class Packet implements Closeable {
    protected byte type;
    protected int length;

    public byte type(){
        return type;
    }

    public int length(){
        return length;
    }
}
