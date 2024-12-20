package net.qiujuer.lesson.sample.server;

import net.qiujuer.lesson.sample.foo.Foo;
import net.qiujuer.lesson.sample.foo.handle.ConnectorHandler;
import net.qiujuer.lesson.sample.foo.handle.ConnectorCloseChain;
import net.qiujuer.lesson.sample.foo.handle.ConnectorStringPacketChain;
import net.qiujuer.library.clink.box.StringReceivePacket;
import net.qiujuer.library.clink.core.Connector;
import net.qiujuer.library.clink.core.SchedulerJob;
import net.qiujuer.library.clink.core.schedule.IdleTimeoutScheduleJob;
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
import java.util.concurrent.TimeUnit;

public class TCPServer implements ServerAcceptor.AcceptListener, Group.GroupMessageAdapter {
    private final int port;
    private ServerAcceptor mServerAcceptor;
    private final List<ConnectorHandler> connectorHandlerList = new ArrayList<>();
    private Selector selector;
    private ServerSocketChannel serverChannel;
    private final File cachePath;
    private final ServerStatistics statistics = new ServerStatistics();
    private final Map<String, Group> groups = new HashMap<>();

    public TCPServer(int port, File cachePath) {
        this.port = port;
        this.cachePath = cachePath;
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

    void broadcast(String str) {
        str = "系统通知：" + str;
        synchronized (connectorHandlerList) {
            ConnectorHandler[] connectorHandlers;
            synchronized (connectorHandlerList) {
                connectorHandlers = connectorHandlerList.toArray(new ConnectorHandler[0]);
            }
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

    /**
     * 获取当前数据状态
     */
    Object[] getStatusString() {
        return new String[]{
                "客户端数量：" + connectorHandlerList.size(),
                "发送数据量：" + statistics.sendSize,
                "接收数据量：" + statistics.receiveSize
        };
    }

    @Override
    public void onNewSocketArrived(SocketChannel channel) {
        try {
            ConnectorHandler connectorHandler = new ConnectorHandler(channel, cachePath);
            System.out.println(connectorHandler.getClientInfo() + ": Connected");

            // 添加统计数据责任链节点
            connectorHandler.getStringPacketChain()
                    .appendLast(statistics.staticsChain())
                    .appendLast(new ParseCommandConnectorStringPacketChain());

            // 添加关闭操作的责任链节点
            connectorHandler.getCloseChain().appendLast(new RemoveQueueOnConnectorClosedChain());

            // 添加空闲任务发送心跳包
            SchedulerJob schedulerJob = new IdleTimeoutScheduleJob(5, TimeUnit.SECONDS, connectorHandler);
            connectorHandler.schedule(schedulerJob);

            synchronized (connectorHandlerList) {
                connectorHandlerList.add(connectorHandler);
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
                // 移除群聊中的客户端
                Group group = groups.get(Foo.DEFAULT_GROUP_NAME);
                group.removeMember(handler);
            }
            return true;
        }
    }

    private class ParseCommandConnectorStringPacketChain extends ConnectorStringPacketChain {

        @Override
        protected boolean consume(ConnectorHandler handler, StringReceivePacket stringReceivePacket) {
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
        protected boolean consumeAgain(ConnectorHandler handler, StringReceivePacket stringReceivePacket) {
            // 捡漏，当第一次未消费，没有加入到群，进行二次消费
            sendMessageToClient(handler, stringReceivePacket.entity());
            return true;
        }
    }
}
