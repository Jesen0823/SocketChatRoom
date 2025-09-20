package demo1;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * UDP搜索者，用于搜索服务支持方
 * */
public class UDPSearcher {
    public static void main(String[] args) throws SocketException, IOException {
        System.out.println("demo1.UDPSearcher start");

        // 搜索方无需指定端口，让系统分配端口
        DatagramSocket socket = new DatagramSocket();
        // 构建请求数据
        String requestData = "HelloKitty:";
        byte[] requestDataBytes = requestData.getBytes();
        DatagramPacket requestPack = new DatagramPacket(
                requestDataBytes,
                requestDataBytes.length
        );
        // 本机端口
        requestPack.setAddress(InetAddress.getLocalHost());
        requestPack.setPort(20000);
        // 给发送端回复
        socket.send(requestPack);


        // 构建接收实体
        final byte[] bufs = new byte[512];
        DatagramPacket receivePack = new DatagramPacket(bufs, bufs.length);
        // 接收
        socket.receive(receivePack);
        // 打印发送者信息，收到的信息
        String ip = receivePack.getAddress().getHostAddress();
        int port = receivePack.getPort();
        int dataLen = receivePack.getLength();
        String data = new String(receivePack.getData(), 0, dataLen);
        System.out.println("demo1.UDPSearcher receive from ip[" + ip + " :" + port + "] data: " + data);
        System.out.println("demo1.UDPSearcher end");

        socket.close();
    }
}
