package net.qiujuer.lesson.sample.server.handle;


import net.qiujuer.lesson.sample.foo.Foo;
import net.qiujuer.library.clink.box.StringReceivePacket;
import net.qiujuer.library.clink.core.Connector;
import net.qiujuer.library.clink.core.Packet;
import net.qiujuer.library.clink.core.ReceivePacket;
import net.qiujuer.library.clink.utils.CloseUtils;

import java.io.File;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Executor;

public class ClientHandler extends Connector{
    private final String clientInfo;
    private final File cachePath;
    private final ConnectorCloseChain closeChain = new DefaultPrintConnectorCloseChain();
    private final ConnectorStringPacketChain stringPacketChain = new DefaultNonConnectorStringChain();
    private final Executor deliveryPool;


    public ClientHandler(SocketChannel socketChannel,  Executor deliveryPool, File cachePath) throws IOException {
        this.deliveryPool = deliveryPool;
        this.clientInfo = socketChannel.getRemoteAddress().toString();
        this.cachePath = cachePath;

        setup(socketChannel);
    }

    public String getClientInfo(){
        return clientInfo;
    }

    public void exit() {
        CloseUtils.close(this);
        closeChain.handle(this,this);
    }

    @Override
    protected File createNewReceiveFile() {
        return Foo.createRandomTemp(cachePath);
    }

    @Override
    public void onChannelClosed(SocketChannel channel) {
        super.onChannelClosed(channel);
        closeChain.handle(this,this);
    }

    @Override
    protected void onReceiveNewPacket(ReceivePacket packet) {
        super.onReceiveNewPacket(packet);
        switch (packet.type()){
            case Packet.TYPE_MEMORY_STRING:
                deliveryStringPacket((StringReceivePacket)packet);
                break;
            default:
                System.out.println("New Packet Received: "+packet.type()+"-"+packet.length());
        }
    }

    private void deliveryStringPacket(StringReceivePacket packet) {
        deliveryPool.execute(()->{
            stringPacketChain.handle(this,packet);
        });
    }

    public ConnectorStringPacketChain getStringPacketChain() {
        return stringPacketChain;
    }

    public ConnectorCloseChain getCloseChain(){
        return closeChain;
    }
}
