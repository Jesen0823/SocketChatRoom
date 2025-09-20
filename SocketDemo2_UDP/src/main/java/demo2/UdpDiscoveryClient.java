package demo2;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

/**
 * 客户端代码（UDP 请求端，主动发现服务端）
 */

// UDP客户端：模拟局域网内的设备（如手机APP），主动发现服务端
public class UdpDiscoveryClient {
    // 服务端的UDP端口，必须与服务器一致
    private static final int SERVER_PORT = 8888;
    // 服务端IP（局域网内可固定，或用广播地址如255.255.255.255）
    private static final String SERVER_IP = "127.0.0.1"; // 本地测试用，实际替换为局域网服务端IP
    // 缓冲区大小：接收服务端回复用
    private static final int BUFFER_SIZE = 1024;
    // 超时时间：避免无限等待（单位：毫秒）
    private static final int TIMEOUT = 3000;

    public static void main(String[] args) {
        DatagramSocket clientSocket = null;
        try {
            // 1. 创建UDP Socket（客户端无需绑定端口，系统会自动分配临时端口）
            clientSocket = new DatagramSocket();
            clientSocket.setSoTimeout(TIMEOUT); // 设置接收超时（超过3秒没回复则报错）
            System.out.println("[C] UDP客户端已经启动，开始寻找服务端...");

            // 2. 构造“发现请求”数据
            String request = "请求发现服务端";
            byte[] requestBuf = request.getBytes(StandardCharsets.UTF_8);

            // 3. 封装请求数据包（指定服务端的端口和ip）
            DatagramPacket requestPacket = new DatagramPacket(
                    requestBuf,
                    requestBuf.length,
                    InetAddress.getByName(SERVER_IP),
                    SERVER_PORT
            );

            // 4. 发送请求给服务端（无连接，直接发送）
            clientSocket.send(requestPacket);
            System.out.println("[C] 已发送发现请求：" + request);


            // 5. 准备接收服务端的回复
            byte[] responseBuf = new byte[BUFFER_SIZE];
            DatagramPacket responsePacket = new DatagramPacket(responseBuf, responseBuf.length);

            // 6. 阻塞等待服务端回复（超时会抛SocketTimeoutException）
            clientSocket.receive(responsePacket);

            // 7. 解析服务端回复
            String response = new String(
                    responsePacket.getData(),
                    0,
                    responsePacket.getLength(),
                    StandardCharsets.UTF_8
            );
            System.out.println("[C] 收到服务端回复：" + response);

        } catch (Exception e) {
            if (e.getMessage().contains("timeout")) {
                System.out.println("[C] 寻找服务端超时（" + TIMEOUT + "ms），请检查服务端是否在线");
            } else {
                e.printStackTrace();
            }
        } finally {
            // 8. 关闭Socket
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        }
    }
}
