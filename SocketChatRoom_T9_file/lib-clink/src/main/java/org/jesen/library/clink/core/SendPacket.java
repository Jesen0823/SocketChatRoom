package org.jesen.library.clink.core;

import java.io.InputStream;

/**
 * 发送包
 * */
public abstract class SendPacket<T extends InputStream> extends Packet<T>{

    /**
     * 取消发送标记
     */
    private boolean isCanceled;

    public boolean isCanceled() {
        return isCanceled;
    }

    public void cancel(){
        isCanceled = true;
    }


}
