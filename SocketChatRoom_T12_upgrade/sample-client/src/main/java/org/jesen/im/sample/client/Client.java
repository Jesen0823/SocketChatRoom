package org.jesen.im.sample.client;

import org.jesen.im.sample.client.bean.ServerInfo;
import org.jesen.im.sample.foo.Foo;
import org.jesen.im.sample.foo.handle.ConnectorHandler;
import org.jesen.im.sample.foo.handle.chain.ConnectorCloseChain;
import org.jesen.library.clink.box.FileSendPacket;
import org.jesen.library.clink.core.Connector;
import org.jesen.library.clink.core.IoContext;
import org.jesen.library.clink.core.ScheduleJob;
import org.jesen.library.clink.core.schedule.IdleTimeoutScheduleJob;
import org.jesen.library.clink.impl.IoSelectorProvider;
import org.jesen.library.clink.impl.SchedulerImpl;
import org.jesen.library.clink.utils.CloseUtils;

import java.io.*;
import java.util.concurrent.TimeUnit;

public class Client {
    public static void main(String[] args) throws IOException {
        File cachePath = Foo.getCacheDir("client");
        IoContext.setup()
                .ioProvider(new IoSelectorProvider())
                .scheduler(new SchedulerImpl(1))
                .start();

        ServerInfo info = UDPSearcher.searchServer(10000);
        System.out.println("Server:" + info);

        if (info != null) {
            TCPClient tcpClient = null;
            try {
                tcpClient = TCPClient.startWith(info, cachePath);
                if (tcpClient == null) {
                    return;
                }
                tcpClient.getCloseChain()
                        .appendLast(new ConnectorCloseChain() {
                            @Override
                            protected boolean consume(ConnectorHandler handler, Connector model) {
                                CloseUtils.close(System.in);
                                return true;
                            }
                        });
                ScheduleJob scheduleJob = new IdleTimeoutScheduleJob(10, TimeUnit.SECONDS,tcpClient);
                tcpClient.schedule(scheduleJob);

                write(tcpClient);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (tcpClient != null) {
                    tcpClient.exit();
                }
            }
        }
        IoContext.close();
    }

    // 默认的键盘输出
    private static void write(TCPClient tcpClient) throws IOException {
        // 构建键盘输入流
        InputStream in = System.in;
        BufferedReader input = new BufferedReader(new InputStreamReader(in));

        do {
            // 键盘读取一行
            String str = input.readLine();
            if (str == null || Foo.COMMAND_EXIT.equalsIgnoreCase(str)) {
                break;
            }
            if (str.length() == 0){
                continue;
            }
            // 发送文件
            if (str.startsWith("--f")) {
                String[] array = str.split(" ");
                if (array.length >= 2) {
                    String filePath = array[1];
                    File file = new File(filePath);
                    if (file.exists() && file.isFile()) {
                        FileSendPacket sendPacket = new FileSendPacket(file);
                        tcpClient.send(sendPacket);
                        continue;
                    }
                }
            }
            // 发送到服务器
            tcpClient.send(str);
        } while (true);
    }
}
