package org.jesen.im.sample.server;

import org.jesen.im.sample.foo.Foo;
import org.jesen.im.sample.foo.constants.TCPConstants;
import org.jesen.library.clink.core.IoContext;
import org.jesen.library.clink.impl.IoSelectorProvider;
import org.jesen.library.clink.impl.SchedulerImpl;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class Server {
    private static final File cachePath = Foo.getCacheDir("server");

    public static void main(String[] args) throws IOException {
        IoContext.setup()
                .ioProvider(new IoSelectorProvider())
                .scheduler(new SchedulerImpl(1))
                .start();

        TCPServer tcpServer = new TCPServer(TCPConstants.PORT_SERVER, cachePath);
        boolean isSucceed = tcpServer.start();
        if (!isSucceed) {
            System.out.println("Start TCP server failed!");
            return;
        }

        UDPProvider.start(TCPConstants.PORT_SERVER);

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        String str;
        do {
            str = bufferedReader.readLine();
            if (str == null
                    || Foo.COMMAND_EXIT.equalsIgnoreCase(str)) {
                break;
            }
            if (str.length()==0){
                continue;
            }
            // 发送字符串
            tcpServer.broadcast(str);
        } while (true);

        UDPProvider.stop();
        tcpServer.stop();

        IoContext.close();
    }
}
