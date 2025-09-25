package org.jesen.im.sample.server;

import org.jesen.library.clink.utils.CloseUtils;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;

public class ServerAcceptor extends Thread {
    private boolean done = false;
    private final AcceptListener listener;
    private final Selector selector;
    private final CountDownLatch latch = new CountDownLatch(1);

    public ServerAcceptor(AcceptListener listener) throws IOException {
        super("Server-Accept-Thread");
        this.listener = listener;
        this.selector = Selector.open();
    }

    boolean awaitRunning() {
        try {
            latch.await();
            return true;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void run() {
        super.run();

        // 已进入run
        latch.countDown();

        Selector selector = this.selector;
        System.out.println("服务器准备就绪～");

        // 等待客户端连接
        do {
            // 得到客户端
            try {
                if (selector.select() == 0) {
                    if (done) {
                        break;
                    }
                    continue;
                }
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    if (done) {
                        break;
                    }
                    SelectionKey key = iterator.next(); // 当前已经就绪的事件
                    iterator.remove();

                    // 检查当前Key状态可用
                    if (key.isAcceptable()) {
                        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
                        // 非阻塞状态拿到一个就绪的客户端
                        SocketChannel socketChannel = serverSocketChannel.accept();
                        listener.onNewSocketArrived(socketChannel);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        } while (!done);

        System.out.println("ServerAcceptor finished.");
    }

    void exit() {
        done = true;
        CloseUtils.close(selector);
    }

    Selector getSelector() {
        return selector;
    }

    interface AcceptListener {
        void onNewSocketArrived(SocketChannel channel);
    }
}
