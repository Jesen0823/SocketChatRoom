package net.qiujuer.lesson.sample.server;

import net.qiujuer.lesson.sample.foo.Foo;
import net.qiujuer.lesson.sample.server.handle.ClientHandler;
import net.qiujuer.lesson.sample.server.handle.ConnectorCloseChain;
import net.qiujuer.lesson.sample.server.handle.ConnectorStringPacketChain;
import net.qiujuer.library.clink.box.StringReceivePacket;
import net.qiujuer.library.clink.core.Connector;
import net.qiujuer.library.clink.utils.CloseUtils;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPServer implements ServerAcceptor.AcceptListener, Group.GroupMessageAdapter {
    private final int port;
    private ServerAcceptor mServerAcceptor;
    private final List<ClientHandler> clientHandlerList = new ArrayList<>();
    private final ExecutorService deliveryPool;
    private Selector selector;
    private ServerSocketChannel serverChannel;
    private final File cachePath;
    private final ServerStatistics statistics = new ServerStatistics();
    private final Map<String, Group> groups = new HashMap<>();

    public TCPServer(int port, File cachePath) {
        this.port = port;
        this.cachePath = cachePath;
        deliveryPool = Executors.newSingleThreadExecutor();
        // 添加默认群
        this.groups.put(Foo.DEFAULT_GROUP_NAME, new Group(Foo.DEFAULT_GROUP_NAME, this));
    }

    public boolean start() {
        try {
            selector = Selector.open();
            serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false); // 设置为非阻塞
            serverChannel.socket().bind(new InetSocketAddress(port)); // 绑定本地端口

            // 注册客户端连接到达事件
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("服务器信息：" + serverChannel.getLocalAddress().toString());

            // 启动客户端监听
            ServerAcceptor serverAcceptor = new ServerAcceptor(this);
            mServerAcceptor = serverAcceptor;
            serverAcceptor.start();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void stop() {
        if (mServerAcceptor != null) {
            mServerAcceptor.exit();
        }

        synchronized (clientHandlerList) {
            for (ClientHandler clientHandler : clientHandlerList) {
                clientHandler.exit();
            }
            clientHandlerList.clear();
        }
        CloseUtils.close(serverChannel);

        deliveryPool.shutdownNow();
    }

    void broadcast(String str) {
        str = "系统通知：" + str;
        synchronized (clientHandlerList) {
            for (ClientHandler clientHandler : clientHandlerList) {
                sendMessageToClient(clientHandler, str);
            }
        }
    }

    @Override
    public void sendMessageToClient(ClientHandler handler, String msg) {
        handler.send(msg);
        statistics.sendSize++;
    }

    /**
     * 获取当前数据状态
     */
    Object[] getStatusString() {
        return new String[]{
                "客户端数量：" + clientHandlerList.size(),
                "发送数据量：" + statistics.sendSize,
                "接收数据量：" + statistics.receiveSize
        };
    }

    @Override
    public void onNewSocketArrived(SocketChannel channel) {
        try {
            ClientHandler clientHandler = new ClientHandler(channel, deliveryPool, cachePath);
            System.out.println(clientHandler.getClientInfo() + ": Connected");

            // 添加统计数据责任链节点
            clientHandler.getStringPacketChain()
                    .appendLast(statistics.staticsChain())
                    .appendLast(new ParseCommandConnectorStringPacketChain());

            // 添加关闭操作的责任链节点
            clientHandler.getCloseChain().appendLast(new RemoveQueueOnConnectorClosedChain());

            synchronized (TCPServer.this) {
                clientHandlerList.add(clientHandler);
                System.out.println("当前客户端数量：" + clientHandlerList.size());
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("客户端连接异常：" + e.getMessage());
        }
    }

    private class RemoveQueueOnConnectorClosedChain extends ConnectorCloseChain {

        @Override
        protected boolean consume(ClientHandler handler, Connector model) {
            synchronized (clientHandlerList) {
                clientHandlerList.remove(handler);
                // 移除群聊中的客户端
                Group group = groups.get(Foo.DEFAULT_GROUP_NAME);
                group.removeMember(handler);
            }
            return true;
        }
    }

    private class ParseCommandConnectorStringPacketChain extends ConnectorStringPacketChain {

        @Override
        protected boolean consume(ClientHandler handler, StringReceivePacket stringReceivePacket) {
            String str = stringReceivePacket.entity();
            if (str.startsWith(Foo.COMMAND_GROUP_JOIN)) {
                Group group = groups.get(Foo.DEFAULT_GROUP_NAME);
                if (group.addMember(handler)) {
                    sendMessageToClient(handler, "Join Group: " + group.getName());
                }
                return true;
            } else if (str.startsWith(Foo.COMMAND_GROUP_LEAVE)) {
                Group group = groups.get(Foo.DEFAULT_GROUP_NAME);
                if (group.removeMember(handler)) {
                    sendMessageToClient(handler, "Level Group: " + group.getName());
                }
                return true;
            }
            return false;
        }

        @Override
        protected boolean consumeAgain(ClientHandler handler, StringReceivePacket stringReceivePacket) {
            // 捡漏，当第一次未消费，没有加入到群，进行二次消费
            sendMessageToClient(handler, stringReceivePacket.entity());
            return true;
        }
    }
}
