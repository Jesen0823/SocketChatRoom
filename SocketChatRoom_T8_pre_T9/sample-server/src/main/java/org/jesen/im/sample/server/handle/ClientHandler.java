package org.jesen.im.sample.server.handle;


import org.jesen.im.sample.foo.Foo;
import org.jesen.library.clink.core.Connector;
import org.jesen.library.clink.core.Packet;
import org.jesen.library.clink.core.ReceivePacket;
import org.jesen.library.clink.utils.CloseUtils;

import java.io.File;
import java.io.IOException;
import java.nio.channels.SocketChannel;

public class ClientHandler extends Connector {
    private final ClientHandlerCallback clientHandlerCallback;
    private final String clientInfo;
    private final File cachePath;

    public ClientHandler(SocketChannel socketChannel, ClientHandlerCallback clientHandlerCallback, File cachePath) throws IOException {
        this.clientHandlerCallback = clientHandlerCallback;
        this.clientInfo = socketChannel.getRemoteAddress().toString();
        this.cachePath = cachePath;
        System.out.println("新客户端连接：" + clientInfo);

        setup(socketChannel);
    }

    public void exit() {
        CloseUtils.close(this);
        System.out.println("客户端已退出：" + clientInfo);
    }

    @Override
    protected File createNewReceiveFile() {
        return Foo.createRandomTemp(cachePath);
    }

    @Override
    protected void onReceiveNewPacket(ReceivePacket packet) {
        super.onReceiveNewPacket(packet);
        if (packet.type() == Packet.TYPE_MEMORY_STRING) {
            String msg = (String) packet.entity();
            System.out.println("TCPServer->ClientHandler, onReceiveNewPacket() " + key.toString() + ": [Type:" +
                    packet.type() + ", Length:" + packet.length() + "], data: " + msg);
            clientHandlerCallback.onMessageArrived(this, msg);
        }
    }

    @Override
    public void onChannelClosed(SocketChannel channel) {
        super.onChannelClosed(channel);
        exitBySelf();
    }

    private void exitBySelf() {
        exit();
        clientHandlerCallback.onSelfClosed(this);
    }

    public interface ClientHandlerCallback {
        // 自己关闭自己
        void onSelfClosed(ClientHandler handler);

        // 客户端收到消息时通知
        void onMessageArrived(ClientHandler handler, String msg);
    }
}
