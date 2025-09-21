package org.jesen.library.clink.core;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

/**
 * 公共数据封装
 * 提供了类型与长度定义
 * */
public abstract class Packet<S extends Closeable> implements Closeable {
    protected byte type;
    protected long length;
    private S stream;

    protected abstract S createStream();

    public byte type(){
        return type;
    }

    public long length(){
        return length;
    }

    public final S open(){
        if(stream == null){
            stream = createStream();
        }
        return stream;
    }

    @Override
    public final void close() throws IOException {
        if (stream != null){
            closeStream(stream);
            stream = null;
        }
    }

    protected void closeStream(S stream) throws IOException {
        if (stream !=null){
            stream.close();
        }
    }

}
