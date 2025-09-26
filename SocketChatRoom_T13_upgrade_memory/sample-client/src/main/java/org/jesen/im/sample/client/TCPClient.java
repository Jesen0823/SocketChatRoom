package org.jesen.im.sample.client;

import org.jesen.im.sample.client.bean.ServerInfo;
import org.jesen.im.sample.foo.handle.ConnectorHandler;
import org.jesen.im.sample.foo.handle.chain.ConnectorStringPacketChain;
import org.jesen.library.clink.box.StringReceivePacket;
import org.jesen.library.clink.utils.CloseUtils;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public class TCPClient extends ConnectorHandler {

    public TCPClient(SocketChannel socketChannel, File cachePath,boolean printReceiveMsg) throws IOException {
        super(socketChannel, cachePath);
        if(printReceiveMsg) {
            getStringPacketChain().appendLast(new PrintStringPacketChain());
        }
    }

    static TCPClient startWith(ServerInfo info, File cacheFile) throws IOException {
        return startWith(info,cacheFile,true);
    }

    static TCPClient startWith(ServerInfo info, File cacheFile,boolean printReceiveMsg) throws IOException {
        SocketChannel socketChannel = SocketChannel.open();

        // 连接本地，端口2000；超时时间3000ms
        socketChannel.connect(new InetSocketAddress(Inet4Address.getByName(info.getAddress()), info.getPort()));

        System.out.println("已发起服务器连接，并进入后续流程～");
        System.out.println("客户端信息：" + socketChannel.getLocalAddress().toString());
        System.out.println("服务器信息：" + socketChannel.getRemoteAddress().toString());

        try {
            return new TCPClient(socketChannel, cacheFile,printReceiveMsg);
        } catch (Exception e) {
            System.out.println("连接异常");
            CloseUtils.close(socketChannel);
        }
        return null;
    }

    private class PrintStringPacketChain extends ConnectorStringPacketChain {
        @Override
        protected boolean consume(ConnectorHandler handler, StringReceivePacket model) {
            String str = model.entity();
            System.out.println("get: " + str);
            return true;
        }
    }
}