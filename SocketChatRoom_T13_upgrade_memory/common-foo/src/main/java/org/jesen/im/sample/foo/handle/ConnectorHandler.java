package org.jesen.im.sample.foo.handle;


import org.jesen.im.sample.foo.Foo;
import org.jesen.im.sample.foo.handle.chain.ConnectorCloseChain;
import org.jesen.im.sample.foo.handle.chain.ConnectorStringPacketChain;
import org.jesen.im.sample.foo.handle.chain.DefaultNonConnectorStringPacketChain;
import org.jesen.im.sample.foo.handle.chain.DefaultPrintConnectorCloseChain;
import org.jesen.library.clink.box.StringReceivePacket;
import org.jesen.library.clink.core.Connector;
import org.jesen.library.clink.core.IoContext;
import org.jesen.library.clink.core.Packet;
import org.jesen.library.clink.core.ReceivePacket;
import org.jesen.library.clink.utils.CloseUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SocketChannel;

public class ConnectorHandler extends Connector {
    private final String clientInfo;
    private final File cachePath;
    private final ConnectorCloseChain closeChain = new DefaultPrintConnectorCloseChain();
    // 普通链表头
    private final ConnectorStringPacketChain stringPacketChain = new DefaultNonConnectorStringPacketChain();

    public ConnectorHandler(SocketChannel socketChannel, File cachePath) throws IOException {
        this.clientInfo = socketChannel.getRemoteAddress().toString();
        this.cachePath = cachePath;

        setup(socketChannel);
    }

    public void exit() {
        CloseUtils.close(this);
    }

    public String getClientInfo() {
        return clientInfo;
    }

    @Override
    protected OutputStream createNewReceiveDirectOutputStream(long length, byte[] headInfo) {
        // 默认创建一个内存存储ByteArrayOutputStream
        return new ByteArrayOutputStream();
    }

    @Override
    protected File createNewReceiveFile(long length, byte[] headInfo) {
        return Foo.createRandomTemp(cachePath);
    }

    @Override
    protected void onReceiveNewPacket(ReceivePacket packet) {
        super.onReceiveNewPacket(packet);
        switch (packet.type()) {
            case Packet.TYPE_MEMORY_STRING:
                deliveryStringPacket((StringReceivePacket) packet);
                break;
            default:
                System.out.println("onReceiveNewPacket, new Packet: " + packet.length());
        }
    }

    private void deliveryStringPacket(StringReceivePacket packet) {
        IoContext.get().getScheduler().delivery(() -> {
            stringPacketChain.handle(this, packet);
        });
    }

    public ConnectorStringPacketChain getStringPacketChain() {
        return stringPacketChain;
    }

    public ConnectorHandleChain getCloseChain() {
        return closeChain;
    }

    @Override
    public void onChannelClosed(SocketChannel channel) {
        super.onChannelClosed(channel);
        closeChain.handle(this, this);
    }
}
