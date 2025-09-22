package org.jesen.im.sample.server;

import org.jesen.im.sample.server.handle.ClientHandler;
import org.jesen.library.clink.utils.CloseUtils;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPServer implements ClientHandler.ClientHandlerCallback, ServerAcceptor.AcceptListener {
    private final int port;
    private List<ClientHandler> clientHandlerList = new ArrayList<>();
    private final ExecutorService forwardExecutor;
    private Selector selector;
    private ServerSocketChannel serverChannel;
    private final File cachePath;
    private ServerAcceptor acceptor;

    private long sendSize;
    private long receiveSize;

    public TCPServer(int port, File cachePath) {
        this.port = port;
        this.cachePath = cachePath;
        forwardExecutor = Executors.newSingleThreadExecutor();
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
        CloseUtils.close(serverChannel);
        CloseUtils.close(selector);

        synchronized (TCPServer.this) {
            for (ClientHandler clientHandler : clientHandlerList) {
                clientHandler.exit();
            }
            clientHandlerList.clear();
        }
        forwardExecutor.shutdownNow();
    }

    public void broadcast(String str) {
        synchronized (TCPServer.class) {
            for (ClientHandler clientHandler : clientHandlerList) {
                clientHandler.send(str);
            }
            sendSize += clientHandlerList.size();
        }
    }

    @Override
    public synchronized void onSelfClosed(ClientHandler handler) {
        clientHandlerList.remove(handler);
    }

    @Override
    public void onMessageArrived(final ClientHandler handler, final String msg) {
        receiveSize++;
        // 输出消息到屏幕
        //System.out.println("Received-" + handler.getClientInfo() + ": " + msg);
        forwardExecutor.execute(new Runnable() {
            @Override
            public void run() {
                synchronized (TCPServer.class) {
                    for (ClientHandler clientHandler : clientHandlerList) {
                        if (clientHandler.equals(handler)) {
                            continue; // 跳过自己
                        }
                        // 发送给其他客户端
                        clientHandler.send(msg);
                        sendSize++;
                    }
                }
            }
        });
    }

    public Object[] getStatusString() {
        return new String[]{
                "Client count: " + clientHandlerList.size(),
                "Send count: " + sendSize,
                "Receive count: " + receiveSize
        };
    }

    @Override
    public void onNewSocketArrived(SocketChannel channel) {
        try {
            ClientHandler clientHandler = new ClientHandler(channel, this, cachePath);
            System.out.println(clientHandler.getClientInfo() + ": Connected!");
            synchronized (TCPServer.this) {
                clientHandlerList.add(clientHandler);
                System.out.println("当前客户端数量：" + clientHandlerList.size());
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("客户端连接异常：" + e.getMessage());
        }

    }
}
