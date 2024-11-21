import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * UDP提供者，用于提供服务
 */
public class UDPProvider {

    public static void main(String[] args) throws SocketException, IOException {
        System.out.println("UDPProvider start");

        // 指定一个端口用于数据接收
        DatagramSocket socket = new DatagramSocket(20000);
        // 构建接收实体
        final byte[] buf = new byte[512];
        DatagramPacket receivePack = new DatagramPacket(buf, buf.length);
        // 接收
        socket.receive(receivePack);

        // 打印发送者信息，收到的信息
        String ip = receivePack.getAddress().getHostAddress();
        int port = receivePack.getPort();
        int dataLen = receivePack.getLength();
        String data = new String(receivePack.getData(), 0, dataLen);
        System.out.println("UDPProvider receive from ip[" + ip + " :" + port + "] data: " + data);


        // 构建回送数据
        String responseData = "Receive data with len:"+dataLen;
        byte[] responseDataBytes = responseData.getBytes();
        DatagramPacket responsePack = new DatagramPacket(
                responseDataBytes,
                responseDataBytes.length,
                receivePack.getAddress(),
                receivePack.getPort()
        );
        // 给发送端回复
        socket.send(responsePack);

        System.out.println("UDPProvider end");

        socket.close();
    }
}
