package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPServer {
    private final int port;
    private ClientListener mListener;

    public TCPServer(int portServer) {
        this.port = portServer;
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
    }

    private static class ClientListener extends Thread {
        private ServerSocket serverSocket;
        private boolean done = false;

        public ClientListener(int port) throws IOException {
            serverSocket = new ServerSocket(port);
            System.out.println("TCPServer ClientListener, 服务器信息：" + serverSocket.getInetAddress() + "port:" + serverSocket.getLocalPort());
        }

        @Override
        public void run() {
            super.run();

            System.out.println("服务器准备就绪~");
            // 等待连接客户端
            do {
                // 得到客户端
                Socket client;
                try {
                    client = serverSocket.accept();
                } catch (IOException e) {
                    continue;
                }
                // 客户端构建异步线程
                ClientHandler clientHandler = new ClientHandler(client);
                clientHandler.start();
            } while (!done);
            System.out.println("服务器已关闭");
        }

        public void exit() {
            done = true;
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 客户端消息处理
     */
    private static class ClientHandler extends Thread {
        private Socket socket;
        private boolean flag = true;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            super.run();
            System.out.println("新客户端连接：" + socket.getInetAddress() + ",Port:" + socket.getPort());
            try {
                // 得到打印流，用于数据输出，服务器回送数据使用
                PrintStream socketOutput = new PrintStream(socket.getOutputStream());
                // 得到输入流，用于接收数据
                BufferedReader socketInput = new BufferedReader((new InputStreamReader(socket.getInputStream())));
                do {
                    // 客户端拿到一条数据
                    String str = socketInput.readLine();
                    if ("bye".equalsIgnoreCase(str)) {
                        flag = false;
                        // 回送
                        socketOutput.println("bye");
                    } else {
                        // 打印屏幕上，并回送数据长度
                        System.out.println(str);
                        socketOutput.println("回送：" + str.length());
                    }
                } while (flag);
                socketInput.close();
                socketOutput.close();
            } catch (Exception e) {
                System.out.println("连接异常断开");
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("客户端已退出， " + socket.getInetAddress() + ",port:" + socket.getPort());
        }
    }
}
