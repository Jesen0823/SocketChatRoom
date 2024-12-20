package net.qiujuer.lesson.sample.server;

import net.qiujuer.lesson.sample.server.handle.ClientHandler;
import net.qiujuer.library.clink.utils.CloseUtils;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPServer implements ClientHandler.ClientHandlerCallback {
    private final int port;
    private ClientListener mListener;
    private List<ClientHandler> clientHandlerList = new ArrayList<>();
    private final ExecutorService forwardExecutor;
    private Selector selector;
    private ServerSocketChannel serverChannel;
    private final File cachePath;

    public TCPServer(int port, File cachePath) {
        this.port = port;
        this.cachePath = cachePath;
        forwardExecutor = Executors.newSingleThreadExecutor();
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
            ClientListener listener = new ClientListener();
            mListener = listener;
            listener.start();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void stop() {
        if (mListener != null) {
            mListener.exit();
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
        }
    }

    @Override
    public synchronized void onSelfClosed(ClientHandler handler) {
        clientHandlerList.remove(handler);
    }

    @Override
    public void onMessageArrived(final ClientHandler handler, final String msg) {
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
                    }
                }
            }
        });
    }

    private class ClientListener extends Thread {
        private boolean done = false;

        @Override
        public void run() {
            super.run();

            Selector selector = TCPServer.this.selector;
            System.out.println("服务器准备就绪～");
            // 等待客户端连接
            do {
                // 得到客户端
                try {
                    if (selector.select() == 0) {
                        if (done) {
                            break;
                        }
                        continue;
                    }
                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while (iterator.hasNext()) {
                        if (done) {
                            break;
                        }
                        SelectionKey key = iterator.next(); // 当前已经就绪的事件
                        iterator.remove();

                        // 检查当前Key状态可用
                        if (key.isAcceptable()) {
                            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
                            // 非阻塞状态拿到一个就绪的客户端
                            SocketChannel socketChannel = serverSocketChannel.accept();
                            try {
                                // 客户端构建异步线程
                                ClientHandler clientHandler = new ClientHandler(socketChannel, TCPServer.this, cachePath);

                                synchronized (TCPServer.this) {
                                    clientHandlerList.add(clientHandler);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                                System.out.println("客户端连接异常：" + e.getMessage());
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } while (!done);

            System.out.println("服务器已关闭！");
        }

        void exit() {
            done = true;
            // 唤醒selector的阻塞
            selector.wakeup();
        }
    }
}
