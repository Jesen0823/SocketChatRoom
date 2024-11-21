package net.qiujuer.library.clink.core;
/**
 * 发送包
 * */
public abstract class SendPacket extends Packet{
    public abstract byte[] bytes();

    private boolean isCanceled;

    public boolean isCanceled() {
        return isCanceled;
    }
}
