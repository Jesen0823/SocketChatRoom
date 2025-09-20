package org.jesen.library.clink.frames;

import org.jesen.library.clink.core.Frame;
import org.jesen.library.clink.core.IoArgs;
import org.jesen.library.clink.core.SendPacket;

import java.io.IOException;

public abstract class AbsSendPacketFrame extends AbsSendFrame {
    protected volatile SendPacket<?> packet;

    public AbsSendPacketFrame(int length, byte type, byte flag, short identifier, SendPacket packet) {
        super(length, type, flag, identifier);
        this.packet = packet;
    }

    /**
     * 获取当前对应的发送Packet
     */
    public synchronized SendPacket getPacket(){
        return packet;
    }

    @Override
    public final synchronized Frame nextFrame() {
        return packet == null ? null : buildNextFrame();
    }

    protected abstract Frame buildNextFrame();

    @Override
    public synchronized boolean handle(IoArgs args) throws IOException {
        if (packet == null && !isSending()) {
            // 已取消，未发送任何数据
            return true;
        }
        return super.handle(args);
    }

    /**
     * 终止
     *
     * @return true 当前帧没有发送任何数据;false 发送了部分
     */
    public final synchronized boolean abort() {
        boolean isSending = isSending();
        if (isSending) {
            fillDirtyDataOnAbort();
        }
        packet = null;
        return !isSending;
    }

    /**
     * 扩展方法，填充假数据
     * */
    protected void fillDirtyDataOnAbort() {

    }


}
