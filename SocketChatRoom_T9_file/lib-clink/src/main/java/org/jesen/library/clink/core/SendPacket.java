package org.jesen.library.clink.core;

import java.io.InputStream;

/**
 * 发送包
 * */
public abstract class SendPacket<T extends InputStream> extends Packet<T>{

    private boolean isCanceled;

    public boolean isCanceled() {
        return isCanceled;
    }


}
