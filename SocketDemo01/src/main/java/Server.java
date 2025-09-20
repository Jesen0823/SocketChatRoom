import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private static void printInfoServer(String msg) {
        System.out.println("[Server] - " + msg);
    }

    public static void main(String[] args) throws IOException {

        ServerSocket serverSocket = new ServerSocket(2000);

        printInfoServer("服务端准备就绪，即将进入后续流程");
        printInfoServer("服务端信息:" + serverSocket.getInetAddress() + ":" + serverSocket.getLocalPort());
        printInfoServer("等待客户端连接...");

        //等待客户端连接
        for (; ; ) {
            // 拿到客户端
            Socket client = serverSocket.accept();
            // 异步处理
            ClientHandler clientHandler = new ClientHandler(client);
            clientHandler.start();
        }

    }

    private static void todo(Socket client) {

    }

    // 异步任务处理
    private static class ClientHandler extends Thread {
        private Socket socket;
        private boolean exit = false;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            super.run();
            printInfoServer("我连接的客户端是：" + socket.getInetAddress() + ":" + socket.getPort());

            try {
                // 打印流，用来回送数据
                PrintStream ps = new PrintStream(socket.getOutputStream());
                // 得到输入流，用于接收数据
                BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                do {
                    // 客户端收到一条信息
                    String str = br.readLine();
                    if ("bye".equalsIgnoreCase(str)) {
                        exit = true;
                        // 客户端回送
                        printInfoServer("客户端说bye了,那我也准备退出");
                        ps.println("bye");
                    } else {
                        // 收到数据，打印展示
                        printInfoServer("收到来自客户端的消息：" + str);
                        // 回送一条给服务端
                        ps.println("客户端，你发送给我的消息有这么长: " + str.length());
                    }
                } while (!exit);

                ps.close();
                br.close();
            } catch (Exception e) {
                printInfoServer("服务器异常断开");
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            printInfoServer("客户端已关闭");
        }
    }
}
