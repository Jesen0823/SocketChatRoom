import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.UUID;

/**
 * UDP提供者，用于提供服务
 */
public class UDPProvider {

    public static void main(String[] args) throws SocketException, IOException {
        // 生成唯一标识
        String sn = UUID.randomUUID().toString();
        Provider provider = new Provider(sn);
        provider.start();

        // 读取任意字符退出
        System.in.read();
        provider.exit();

    }

    private static class Provider extends Thread {
        private final String sn;
        private boolean done = false;
        private DatagramSocket ds = null;

        public Provider(String sn) {
            super();
            this.sn = sn;
        }

        @Override
        public void run() {
            super.run();
            System.out.println("UDPProvider start");
            try {
                // 构建监听20000端口
                ds = new DatagramSocket(20000);
                while (!done) {
                    // 指定一个端口用于数据接收
                    // 构建接收实体
                    final byte[] buf = new byte[512];
                    DatagramPacket receivePack = new DatagramPacket(buf, buf.length);
                    // 接收
                    ds.receive(receivePack);

                    // 打印发送者信息，收到的信息
                    String ip = receivePack.getAddress().getHostAddress();
                    int port = receivePack.getPort();
                    int dataLen = receivePack.getLength();
                    String data = new String(receivePack.getData(), 0, dataLen);
                    System.out.println("UDPProvider receive from ip[" + ip + " :" + port + "] data: " + data);

                    // 解析端口
                    int responsePort = MessageCreator.parsePort(data);
                    if (responsePort != -1) {
                        // 构建回送数据
                        String responseData = MessageCreator.buildWithSN(sn);
                        byte[] responseDataBytes = responseData.getBytes();
                        DatagramPacket responsePack = new DatagramPacket(
                                responseDataBytes,
                                responseDataBytes.length,
                                receivePack.getAddress(),
                                responsePort
                        );
                        // 给发送端回复
                        ds.send(responsePack);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                close();
            }

            System.out.println("UDPProvider end");
        }

        private void close() {
            if (ds != null) {
                ds.close();
                ds = null;
            }
        }

        void exit() {
            done = true;
            close();
        }
    }
}
