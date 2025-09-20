package demo2;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

/**
 * UDP 实用案例：局域网设备发现
 * <p>
 * UDP 最适合 “低延迟、轻量级、允许少量数据丢失” 的场景，比如 “局域网内客户端自动发现服务端”（如智能家居设备找网关、打印机发现电脑）。该场景下，客户端只需发送 “发现请求”，服务端回复 “自身地址”，无需建立连接，UDP 的低开销优势明显。
 */

// UDP服务端：模拟局域网内的设备（如智能家居网关），等待客户端发现
public class UdpDiscoveryServer {
    // 服务监听的UDP端口（固定，客户端需要知道此端口）
    private static final int SERVER_PORT = 8888;
    // 缓冲区大小：接收客户端请求用到
    private static final int BUFFER_SIZE = 1024;

    public static void main(String[] args) {
        DatagramSocket serverSocket = null;
        try {
            // 1. 创建UDP Socket，并绑定固定端口（必须绑定，否则客户端无法找到服务端）
            serverSocket = new DatagramSocket(SERVER_PORT);
            System.out.println("[S] udp 服务端已经启动，监听端口：" + SERVER_PORT);

            // 2. 准备接收缓冲区和数据包（接收客户端的请求
            byte[] receiveBuf = new byte[BUFFER_SIZE];
            DatagramPacket receivePacket = new DatagramPacket(receiveBuf, receiveBuf.length);

            // 循环监听，持续接收客户端请求
            while (true) {
                // 3. 阻塞等待接收客户端数据包（无请求时会一直卡在这里）
                serverSocket.receive(receivePacket);

                // 4. 解析客户端请求（提取请求内容、客户端地址和端口）
                // 注意：数据包的实际长度可能小于缓冲区大小，需用getLength()获取有效数据
                String request = new String(receivePacket.getData(), 0, receivePacket.getLength(), StandardCharsets.UTF_8);
                InetAddress clientIp = receivePacket.getAddress(); // 客户端IP
                int clientPort = receivePacket.getPort();         // 客户端端口（系统随机分配）
                System.out.println("\n[S] 收到客户端请求：" + request);
                System.out.println("[S] 客户端地址：" + clientIp + ":" + clientPort);

                // 5. 构造回复内容（告诉客户端服务端的IP和端口）
                String serverIp = InetAddress.getLocalHost().getHostAddress(); // 服务端自身IP
                String response = "服务端已发现！地址：" + serverIp + ":" + SERVER_PORT;
                byte[] responseBuf = response.getBytes(StandardCharsets.UTF_8);

                // 6. 封装回复数据包（需指定客户端的IP和端口，否则客户端收不到）
                DatagramPacket responsePacket = new DatagramPacket(
                        responseBuf,
                        responseBuf.length,
                        clientIp,
                        clientPort
                );

                // 7. 发送回复给客户端
                serverSocket.send(responsePacket);
                System.out.println("[S] 已回复客户端：" + response);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 8. 关闭Socket（实际服务端通常不关闭，此处为示例完整性）
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        }
    }
}
