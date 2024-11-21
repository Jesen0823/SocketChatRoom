package net.qiujuer.lesson.sample.server;

import net.qiujuer.lesson.sample.server.handle.ClientHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCPServer implements ClientHandler.ClientHandlerCallback {
    private final int port;
    private ClientListener mListener;
    private List<ClientHandler> clientHandlerList = new ArrayList<>();
    private final ExecutorService forwardExecutor;

    public TCPServer(int port) {
        this.port = port;
        forwardExecutor = Executors.newSingleThreadExecutor();
    }

    public boolean start() {
        try {
            ClientListener listener = new ClientListener(port);
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

        synchronized (TCPServer.class) {
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
        System.out.println("Received-" + handler.getClientInfo() + ": " + msg);
        forwardExecutor.execute(new Runnable() {
            @Override
            public void run() {
                synchronized (TCPServer.class){
                    for (ClientHandler clientHandler : clientHandlerList) {
                        if(clientHandler.equals(handler)){
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
        private ServerSocket server;
        private boolean done = false;

        private ClientListener(int port) throws IOException {
            server = new ServerSocket(port);
            System.out.println("服务器信息：" + server.getInetAddress() + " P:" + server.getLocalPort());
        }

        @Override
        public void run() {
            super.run();

            System.out.println("服务器准备就绪～");
            // 等待客户端连接
            do {
                // 得到客户端
                Socket client;
                try {
                    client = server.accept();
                } catch (IOException e) {
                    continue;
                }
                try {
                    // 客户端构建异步线程
                    ClientHandler clientHandler = new ClientHandler(client, TCPServer.this);
                    // 读取数据并打印
                    clientHandler.readToPrint();
                    synchronized (TCPServer.class) {
                        clientHandlerList.add(clientHandler);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("客户端连接异常：" + e.getMessage());
                }
            } while (!done);

            System.out.println("服务器已关闭！");
        }

        void exit() {
            done = true;
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
