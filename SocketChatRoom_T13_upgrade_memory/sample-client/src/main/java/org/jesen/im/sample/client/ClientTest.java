package org.jesen.im.sample.client;

import org.jesen.im.sample.client.bean.ServerInfo;
import org.jesen.im.sample.foo.Foo;
import org.jesen.im.sample.foo.handle.ConnectorHandler;
import org.jesen.im.sample.foo.handle.chain.ConnectorCloseChain;
import org.jesen.library.clink.core.Connector;
import org.jesen.library.clink.core.IoContext;
import org.jesen.library.clink.impl.IoSelectorProvider;
import org.jesen.library.clink.impl.IoStealingSelectorProvider;
import org.jesen.library.clink.impl.SchedulerImpl;
import org.jesen.library.clink.utils.CloseUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClientTest {
    private static volatile boolean done;
    // 不考虑发送消耗，并发量：2000*4/400*1000 = 2w/s 算上来回2次数据解析：4w/s
    private static final int CLIENT_SIZE = 2000;      // 2000个客户端
    private static final int SEND_THREAD_SIZE = 4;    // 4个线程
    private static final int SEND_THREAD_DELAY = 400; // 每个线程间隔400ms

    public static void main(String[] args) throws IOException {
        ServerInfo info = UDPSearcher.searchServer(10000);
        System.out.println("Server:" + info);
        if (info == null) {
            return;
        }

        File cachePath = Foo.getCacheDir("client/test");
        IoContext.setup()
                //.ioProvider(new IoSelectorProvider())
                .ioProvider(new IoStealingSelectorProvider(3))
                .scheduler(new SchedulerImpl(1))
                .start();

        // 当前连接数量
        int size = 0;
        final List<TCPClient> tcpClients = new ArrayList<>(CLIENT_SIZE);

        // 关闭时移除
        final ConnectorCloseChain closeChain = new ConnectorCloseChain() {
            @Override
            protected boolean consume(ConnectorHandler handler, Connector connector) {
                //noinspection SuspiciousMethodCalls
                tcpClients.remove(handler);
                if (tcpClients.size() == 0) {
                    CloseUtils.close(System.in);
                }
                return false;
            }
        };

        for (int i = 0; i < CLIENT_SIZE; i++) {
            try {
                TCPClient tcpClient = TCPClient.startWith(info, cachePath, false);
                if (tcpClient == null) {
                    throw new NullPointerException();
                }

                // 添加关闭链式节点
                tcpClient.getCloseChain().appendLast(closeChain);
                tcpClients.add(tcpClient);

                System.out.println("连接成功：" + (++size));

            } catch (IOException | NullPointerException e) {
                System.out.println("连接异常");
                break;
            }
        }

        System.in.read();

        Runnable runnable = () -> {
            while (!done) {
                TCPClient[] copyClients = tcpClients.toArray(new TCPClient[0]);
                for (TCPClient client : copyClients) {
                    client.send("HuHuHu~~");
                }

                if (SEND_THREAD_DELAY > 0) {
                    try {
                        Thread.sleep(SEND_THREAD_DELAY);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        };

        List<Thread> threads = new ArrayList<>(SEND_THREAD_SIZE);
        for (int i = 0; i < SEND_THREAD_SIZE; i++) {
            Thread thread = new Thread(runnable, "ClientTest-send-thread" + i);
            thread.start();
            threads.add(thread);
        }

        System.in.read();

        // 等待线程完成
        done = true;

        // 客户端结束操作
        TCPClient[] exitClients = tcpClients.toArray(new TCPClient[0]);
        for (TCPClient tcpClient : exitClients) {
            tcpClient.exit();
        }

        IoContext.close();

        // 强制结束处于等待的线程
        for (Thread thread : threads) {
            try {
                thread.interrupt();
            } catch (Exception ignored) {
            }
        }
    }
}

