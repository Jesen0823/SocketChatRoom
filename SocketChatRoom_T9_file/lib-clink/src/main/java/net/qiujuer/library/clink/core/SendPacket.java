package net.qiujuer.library.clink.core;

import net.qiujuer.library.clink.utils.CloseUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * 发送包
 */
public abstract class SendPacket<T extends InputStream> extends Packet<T> {

    private boolean isCanceled;

    public boolean isCanceled() {
        return isCanceled;
    }
}
