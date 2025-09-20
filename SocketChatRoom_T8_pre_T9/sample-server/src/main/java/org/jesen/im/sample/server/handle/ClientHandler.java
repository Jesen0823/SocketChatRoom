package org.jesen.im.sample.server.handle;


import org.jesen.library.clink.core.Connector;
import org.jesen.library.clink.utils.CloseUtils;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public class ClientHandler extends Connector{
    private final ClientHandlerCallback clientHandlerCallback;
    private final String clientInfo;

    public ClientHandler(SocketChannel socketChannel, ClientHandlerCallback clientHandlerCallback) throws IOException {
        this.clientHandlerCallback = clientHandlerCallback;
        this.clientInfo = socketChannel.getRemoteAddress().toString();
        System.out.println("新客户端连接：" + clientInfo);

        setup(socketChannel);
    }

    public void exit() {
        CloseUtils.close(this);
        System.out.println("客户端已退出：" + clientInfo);
    }

    @Override
    public void onChannelClosed(SocketChannel channel) {
        super.onChannelClosed(channel);
        exitBySelf();
    }

    @Override
    protected void onReceiveNewMessage(String str) {
        super.onReceiveNewMessage(str);
        clientHandlerCallback.onMessageArrived(this,str);
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
