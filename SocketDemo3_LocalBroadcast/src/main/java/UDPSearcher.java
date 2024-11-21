import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * UDP搜索者，用于搜索服务支持方
 */
public class UDPSearcher {
    private static final int LISTEN_PORT = 30000;

    public static void main(String[] args) throws SocketException, IOException, InterruptedException {
        System.out.println("UDPSearcher start");

        // 监听3000端口，等待接收各设备回复
        Listener listener = listen();
        // 向各设备发送广播
        sendBroadcast();

        // 读取任意键盘信息退出
        System.in.read();
        List<Device> devices = listener.getDevicesAndClose();
        for (Device device : devices) {
            System.out.println("UDPSearcher device: "+device.toString());
        }
        System.out.println("UDPSearcher end");
    }

    private static Listener listen() throws InterruptedException {
        System.out.println("UDPSearcher listener start");
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Listener listener = new Listener(LISTEN_PORT,countDownLatch);
        listener.start();

        countDownLatch.await();
        return listener;
    }

    private static void sendBroadcast() throws IOException {
        System.out.println("UDPSearcher sendBroadcast start");

        // 搜索方无需指定端口，让系统分配端口
        DatagramSocket socket = new DatagramSocket();
        // 构建请求数据
        String requestData = MessageCreator.buildWithPort(LISTEN_PORT);
        byte[] requestDataBytes = requestData.getBytes();
        DatagramPacket requestPack = new DatagramPacket(
                requestDataBytes,
                requestDataBytes.length
        );
        // 本机端口
        requestPack.setAddress(InetAddress.getByName("255.255.255.255"));
        requestPack.setPort(20000);
        // 给发送端回复
        socket.send(requestPack);
        socket.close();

        System.out.println("UDPSearcher sendBroadcast end");
    }

    private static class Device {
        int port;
        String ip;
        String sn;

        public Device(int port, String ip, String sn) {
            this.port = port;
            this.ip = ip;
            this.sn = sn;
        }

        @Override
        public String toString() {
            return "Device{" +
                    "port=" + port +
                    ", ip='" + ip + '\'' +
                    ", sn='" + sn + '\'' +
                    '}';
        }
    }

    private static class Listener extends Thread {
        private final int listenerPort;
        private final CountDownLatch countDownLatch;
        private final List<Device> deviceList = new ArrayList<>();
        private boolean done = false;
        private DatagramSocket ds = null;

        public Listener(int listenerPort, CountDownLatch countDownLatch) {
            super();
            this.listenerPort = listenerPort;
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void run() {
            super.run();

            countDownLatch.countDown();
            try {
                // 监听回送端口
                ds = new DatagramSocket(listenerPort);
                while (!done) {
                    // 构建接收实体
                    final byte[] bufs = new byte[512];
                    DatagramPacket receivePack = new DatagramPacket(bufs, bufs.length);
                    // 接收
                    ds.receive(receivePack);
                    // 打印发送者信息，收到的信息
                    String ip = receivePack.getAddress().getHostAddress();
                    int port = receivePack.getPort();
                    int dataLen = receivePack.getLength();
                    String data = new String(receivePack.getData(), 0, dataLen);
                    System.out.println("UDPSearcher receive from ip[" + ip + " :" + port + "] data: " + data);

                    String sn = MessageCreator.parseSN(data);
                    if (sn != null) {
                        Device device = new Device(port, ip, sn);
                        deviceList.add(device);
                    }
                }

            } catch (Exception e) {

            } finally {
                close();
            }
            System.out.println("UDPSearcher listener end");
        }

        private void close() {
            if (ds != null) {
                ds.close();
                ds = null;
            }
        }

        List<Device> getDevicesAndClose() {
            done = true;
            close();
            return deviceList;
        }
    }
}
