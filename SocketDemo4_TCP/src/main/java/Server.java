import java.io.*;
import java.lang.reflect.Array;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

public class Server {
    private static final int PORT = 20000;

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = createServerSocket();

        initServerSocket(serverSocket);

        System.out.println("[S] 服务端准备就绪，即将进入后续流程");
        System.out.println("[S] 服务端信息：" + serverSocket.getInetAddress() + ":" + serverSocket.getLocalPort());
        System.out.println("[S] 等待客户端连接...");

        // 绑定到本地端口上
        serverSocket.bind(new InetSocketAddress(Inet4Address.getLocalHost(), PORT), 50);

        //等待客户端连接
        for (; ; ) {
            // 拿到客户端
            Socket client = serverSocket.accept();
            // 异步处理
            ClientHandler clientHandler = new ClientHandler(client);
            clientHandler.start();
        }

    }

    private static ServerSocket createServerSocket() throws IOException {
        // 创建基础的ServerSocket
        ServerSocket serverSocket = new ServerSocket();

        // 绑定到本地端口20000上，并且设置当前可允许等待链接的队列为50个
        //serverSocket = new ServerSocket(PORT);

        // 等效于上面的方案，队列设置为50个
        //serverSocket = new ServerSocket(PORT, 50);

        // 与上面等同
        // serverSocket = new ServerSocket(PORT, 50, Inet4Address.getLocalHost());

        return serverSocket;
    }

    private static void initServerSocket(ServerSocket serverSocket) throws IOException {
        // 是否复用未完全关闭的地址端口
        serverSocket.setReuseAddress(true);

        // 等效Socket#setReceiveBufferSize
        serverSocket.setReceiveBufferSize(64 * 1024 * 1024);

        // 设置serverSocket#accept超时时间
        // serverSocket.setSoTimeout(2000);

        // 设置性能参数：短链接，延迟，带宽的相对重要性
        serverSocket.setPerformancePreferences(1, 1, 1);
    }

    private static void todo(Socket client) {

    }

    // 异步任务处理
    private static class ClientHandler extends Thread {
        private Socket socket;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            super.run();
            System.out.println("[S] 客户端信息：" + socket.getInetAddress() + ":" + socket.getPort());

            try {
                // 得到套接字流
                OutputStream outputStream = socket.getOutputStream();
                InputStream inputStream = socket.getInputStream();

                byte[] buffer = new byte[256];
                int readCount = inputStream.read(buffer);
                // 按顺序读取
                ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, 0, readCount);
                byte be = byteBuffer.get();
                char c = byteBuffer.getChar();
                int i = byteBuffer.getInt();
                boolean bl = byteBuffer.get() == 1;
                long l = byteBuffer.getLong();
                float f = byteBuffer.getFloat();
                double db = byteBuffer.getDouble();
                int pos = byteBuffer.position();
                String str = new String(buffer, pos, readCount - pos - 1);

                if (readCount > 0) {
                    System.out.println("[S] 收到数量：" + readCount + ", 数据：\n"
                            + be + "\n"
                            + c + "\n"
                            + i + "\n"
                            + bl + "\n"
                            + l + "\n"
                            + f + "\n"
                            + db + "\n"
                            + str + "\n");
                    // 回送原数据
                    outputStream.write(buffer, 0, readCount);
                } else {
                    System.out.println("[S] 收到数量：" + readCount);
                    // 回送空数据
                    outputStream.write(new byte[]{0});
                }
                inputStream.close();
                outputStream.close();
            } catch (Exception e) {
                System.out.println("[S] 服务器异常断开");
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("[S] 客户端已关闭");
        }
    }
}
