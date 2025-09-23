package org.jesen.im.sample.server.handle;


import org.jesen.im.sample.foo.Foo;
import org.jesen.im.sample.server.handle.chain.ConnectorCloseChain;
import org.jesen.im.sample.server.handle.chain.ConnectorStringPacketChain;
import org.jesen.im.sample.server.handle.chain.DefaultNonConnectorStringPacketChain;
import org.jesen.im.sample.server.handle.chain.DefaultPrintConnectorCloseChain;
import org.jesen.library.clink.box.StringReceivePacket;
import org.jesen.library.clink.core.Connector;
import org.jesen.library.clink.core.Packet;
import org.jesen.library.clink.core.ReceivePacket;
import org.jesen.library.clink.utils.CloseUtils;

import java.io.File;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Executor;

public class ClientHandler extends Connector {
    private final String clientInfo;
    private final File cachePath;
    private final ConnectorCloseChain closeChain = new DefaultPrintConnectorCloseChain();
    // 普通链表头
    private final ConnectorStringPacketChain stringPacketChain = new DefaultNonConnectorStringPacketChain();
    private final Executor deliveryPool;


    public ClientHandler(SocketChannel socketChannel, File cachePath, Executor deliveryPool) throws IOException {
        this.deliveryPool = deliveryPool;
        this.clientInfo = socketChannel.getRemoteAddress().toString();
        this.cachePath = cachePath;

        setup(socketChannel);
    }

    public void exit() {
        CloseUtils.close(this);
        closeChain.handle(this, this);
    }

    public String getClientInfo() {
        return clientInfo;
    }

    @Override
    protected File createNewReceiveFile() {
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
        deliveryPool.execute(() -> {
            stringPacketChain.handle(this, packet);
        });
    }

    public ConnectorStringPacketChain getStringPacketChain(){
        return stringPacketChain;
    }

    public ConnectorHandleChain getCloseChain(){
        return closeChain;
    }

    @Override
    public void onChannelClosed(SocketChannel channel) {
        super.onChannelClosed(channel);
        closeChain.handle(this, this);
    }
}
