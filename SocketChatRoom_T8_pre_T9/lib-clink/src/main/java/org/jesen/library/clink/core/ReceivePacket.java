package org.jesen.library.clink.core;

/**
 * 接受包
 * */
public abstract class ReceivePacket extends Packet {
    public abstract void save(byte[] bytes,int count);
}
