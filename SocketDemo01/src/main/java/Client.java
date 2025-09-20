import java.io.*;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.Socket;

public class Client {

    private static void printInfoClient(String msg) {
        System.out.println("[Client] - " + msg);
    }

    public static void main(String[] args) throws IOException {
        Socket socket = new Socket();
        // 读取流超时时间
        socket.setSoTimeout(3000);
        // 连接超时3000，连接本地端口2000
        socket.connect(new InetSocketAddress(Inet4Address.getLocalHost(), 2000), 3000);

        printInfoClient("已发起服务器连接，即将进入后续流程");
        printInfoClient("客户端信息:" + socket.getLocalAddress() + ":" + socket.getLocalPort());
        printInfoClient("服务端信息:" + socket.getInetAddress() + ":" + socket.getPort());

        try {
            // 发送数据
            todo(socket);
        } catch (Exception e) {
            printInfoClient("异常关闭");
        }
        socket.close();
        printInfoClient("客户端已退出");
    }

    private static void todo(Socket client) throws IOException {
        // 构建键盘输入流
        InputStream ins = System.in;
        BufferedReader br = new BufferedReader(new InputStreamReader(ins));

        // 客户端输出流
        OutputStream outputStream = client.getOutputStream();
        // 转换为打印流
        PrintStream ps = new PrintStream(outputStream);

        // 服务端输入流
        InputStream inss = client.getInputStream();
        BufferedReader brs = new BufferedReader(new InputStreamReader(inss));

        boolean exit = false;
        do {
            // 从键盘读取一行
            String str = br.readLine();
            // 发送到服务器
            ps.println("服务端，我是客户端, 我想说: " + str);

            // 从服务器读取一行返回信息
            String echo = brs.readLine();
            if ("bye".equalsIgnoreCase(echo)) {
                printInfoClient("服务端说bye,那我准备退出");
                exit = true;
            } else {
                printInfoClient("收到来自服务端消息:" + echo);
            }
        } while (!exit);

        ps.close();
        brs.close();

    }
}
