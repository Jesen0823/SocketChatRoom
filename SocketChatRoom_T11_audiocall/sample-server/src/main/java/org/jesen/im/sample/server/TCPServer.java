package org.jesen.im.sample.server;

import org.jesen.im.sample.foo.Foo;
import org.jesen.im.sample.foo.handle.ConnectorHandler;
import org.jesen.im.sample.foo.handle.chain.ConnectorCloseChain;
import org.jesen.im.sample.foo.handle.chain.ConnectorStringPacketChain;
import org.jesen.library.clink.box.StringReceivePacket;
import org.jesen.library.clink.core.Connector;
import org.jesen.library.clink.core.ScheduleJob;
import org.jesen.library.clink.core.schedule.IdleTimeoutScheduleJob;
import org.jesen.library.clink.utils.CloseUtils;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class TCPServer implements ServerAcceptor.AcceptListener, Group.GroupMessageAdapter {
    private final int port;
    private final List<ConnectorHandler> connectorHandlerList = new ArrayList<>();
    private ServerSocketChannel serverChannel;
    private final File cachePath;
    private ServerAcceptor acceptor;

    private final ServerStatistics statistics = new ServerStatistics();
    private final Map<String, Group> groups = new HashMap<>();

    public TCPServer(int port, File cachePath) {
        this.port = port;
        this.cachePath = cachePath;

        this.groups.put(Foo.DEFAULT_GROUP_NAME, new Group(Foo.DEFAULT_GROUP_NAME, this));
    }

    public boolean start() {
        try {
            ServerAcceptor acceptor = new ServerAcceptor(this);
            ServerSocketChannel server = ServerSocketChannel.open();
            server.configureBlocking(false);
            // 绑定本地端口
            server.socket().bind(new InetSocketAddress(port));
            // 注册客户端连接到达监听
            server.register(acceptor.getSelector(), SelectionKey.OP_ACCEPT);
            this.serverChannel = server;
            this.acceptor = acceptor;

            acceptor.start();

            if (acceptor.awaitRunning()) {
                System.out.println("服务器准备就绪～");
                System.out.println("服务器信息：" + serverChannel.getLocalAddress().toString());
                return true;
            } else {
                System.out.println("启动异常！");
                return false;
            }

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void stop() {
        if (acceptor != null) {
            acceptor.exit();
        }

        ConnectorHandler[] connectorHandlers;
        synchronized (connectorHandlerList) {
            connectorHandlers = connectorHandlerList.toArray(new ConnectorHandler[0]);
            connectorHandlerList.clear();
        }
        for (ConnectorHandler connectorHandler : connectorHandlers) {
            connectorHandler.exit();
        }
        CloseUtils.close(serverChannel);
    }

    public void broadcast(String str) {
        str = "系统通知：" + str;
        ConnectorHandler[] connectorHandlers;
        synchronized (connectorHandlerList) {
            connectorHandlers = connectorHandlerList.toArray(new ConnectorHandler[0]);
            for (ConnectorHandler connectorHandler : connectorHandlers) {
                sendMessageToClient(connectorHandler, str);
            }
        }
    }

    @Override
    public void sendMessageToClient(ConnectorHandler handler, String msg) {
        handler.send(msg);
        statistics.sendSize++;
    }

    public Object[] getStatusString() {
        return new String[]{
                "Client count: " + connectorHandlerList.size(),
                "Send count: " + statistics.sendSize,
                "Receive count: " + statistics.receiveSize
        };
    }

    @Override
    public void onNewSocketArrived(SocketChannel channel) {
        try {
            ConnectorHandler ConnectorHandler = new ConnectorHandler(channel, cachePath);
            System.out.println("Client [" + ConnectorHandler.getClientInfo() + "] : Connected!");

            ConnectorHandler.getStringPacketChain()
                    .appendLast(statistics.statisticsChain())
                    .appendLast(new ParseCommandConnectorStringPacketChain());

            ConnectorHandler.getCloseChain()
                    .appendLast(new RemoveQueueOnConnectorClosedChain());

            ScheduleJob scheduleJob = new IdleTimeoutScheduleJob(5, TimeUnit.SECONDS, ConnectorHandler);
            ConnectorHandler.schedule(scheduleJob);

            synchronized (connectorHandlerList) {
                connectorHandlerList.add(ConnectorHandler);
                System.out.println("当前客户端数量：" + connectorHandlerList.size());
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("客户端连接异常：" + e.getMessage());
        }

    }

    private class RemoveQueueOnConnectorClosedChain extends ConnectorCloseChain {
        @Override
        protected boolean consume(ConnectorHandler handler, Connector model) {
            synchronized (connectorHandlerList) {
                connectorHandlerList.remove(handler);
            }
            // 移除群聊的客户端
            Group group = groups.get(Foo.DEFAULT_GROUP_NAME);
            group.removeMember(handler);
            return true;
        }
    }

    private class ParseCommandConnectorStringPacketChain extends ConnectorStringPacketChain {

        @Override
        protected boolean consume(ConnectorHandler handler, StringReceivePacket model) {
            String str = model.entity();
            if (str.startsWith(Foo.COMMAND_GROUP_JOIN)) {
                Group group = groups.get(Foo.DEFAULT_GROUP_NAME);
                if (group.addMember(handler)) {
                    sendMessageToClient(handler, "Join Group [" + group.getName() + "】 success.");
                }
                return true;
            } else if (str.startsWith(Foo.COMMAND_GROUP_LEAVE)) {
                Group group = groups.get(Foo.DEFAULT_GROUP_NAME);
                if (group.removeMember(handler)) {
                    sendMessageToClient(handler, "Leave Group [" + group.getName() + "】 success.");
                }
                return true;
            }
            return false;
        }

        @Override
        protected boolean consumeAgain(ConnectorHandler handler, StringReceivePacket model) {
            // 第一次没能消费，因为没有节点消费。此时二次消费，直接原路返回
            sendMessageToClient(handler, model.entity());
            return true;
        }
    }
}
